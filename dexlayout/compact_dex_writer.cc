/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "compact_dex_writer.h"

#include "android-base/stringprintf.h"
#include "base/logging.h"
#include "base/time_utils.h"
#include "dex/compact_dex_file.h"
#include "dex/compact_offset_table.h"
#include "dexlayout.h"

namespace art {

CompactDexWriter::CompactDexWriter(DexLayout* dex_layout)
    : DexWriter(dex_layout, /*compute_offsets=*/ true) {
  CHECK(GetCompactDexLevel() != CompactDexLevel::kCompactDexLevelNone);
}

CompactDexLevel CompactDexWriter::GetCompactDexLevel() const {
  return dex_layout_->GetOptions().compact_dex_level_;
}

CompactDexWriter::Container::Container()
    : data_item_dedupe_(&data_section_) {}

uint32_t CompactDexWriter::WriteDebugInfoOffsetTable(Stream* stream) {
  const uint32_t start_offset = stream->Tell();
  // Debug offsets for method indexes. 0 means no debug info.
  std::vector<uint32_t> debug_info_offsets(header_->MethodIds().Size(), 0u);

  static constexpr InvokeType invoke_types[] = {
    kDirect,
    kVirtual
  };

  for (InvokeType invoke_type : invoke_types) {
    for (auto& class_def : header_->ClassDefs()) {
      // Skip classes that are not defined in this dex file.
      dex_ir::ClassData* class_data = class_def->GetClassData();
      if (class_data == nullptr) {
        continue;
      }
      for (auto& method : *(invoke_type == InvokeType::kDirect
                                ? class_data->DirectMethods()
                                : class_data->VirtualMethods())) {
        const dex_ir::MethodId* method_id = method.GetMethodId();
        dex_ir::CodeItem* code_item = method.GetCodeItem();
        if (code_item != nullptr && code_item->DebugInfo() != nullptr) {
          const uint32_t debug_info_offset = code_item->DebugInfo()->GetOffset();
          const uint32_t method_idx = method_id->GetIndex();
          if (debug_info_offsets[method_idx] != 0u) {
            CHECK_EQ(debug_info_offset, debug_info_offsets[method_idx]);
          }
          debug_info_offsets[method_idx] = debug_info_offset;
        }
      }
    }
  }

  std::vector<uint8_t> data;
  debug_info_base_ = 0u;
  debug_info_offsets_table_offset_ = 0u;
  CompactOffsetTable::Build(debug_info_offsets,
                            &data,
                            &debug_info_base_,
                            &debug_info_offsets_table_offset_);
  // Align the table and write it out.
  stream->AlignTo(CompactOffsetTable::kAlignment);
  debug_info_offsets_pos_ = stream->Tell();
  stream->Write(data.data(), data.size());

  // Verify that the whole table decodes as expected and measure average performance.
  const bool kMeasureAndTestOutput = dex_layout_->GetOptions().verify_output_;
  if (kMeasureAndTestOutput && !debug_info_offsets.empty()) {
    uint64_t start_time = NanoTime();
    stream->Begin();
    CompactOffsetTable::Accessor accessor(stream->Begin() + debug_info_offsets_pos_,
                                          debug_info_base_,
                                          debug_info_offsets_table_offset_);

    for (size_t i = 0; i < debug_info_offsets.size(); ++i) {
      CHECK_EQ(accessor.GetOffset(i), debug_info_offsets[i]);
    }
    uint64_t end_time = NanoTime();
    VLOG(dex) << "Average lookup time (ns) for debug info offsets: "
              << (end_time - start_time) / debug_info_offsets.size();
  }

  return stream->Tell() - start_offset;
}

CompactDexWriter::ScopedDataSectionItem::ScopedDataSectionItem(Stream* stream,
                                                               dex_ir::Item* item,
                                                               size_t alignment,
                                                               Deduper* deduper)
    : stream_(stream),
      item_(item),
      alignment_(alignment),
      deduper_(deduper),
      start_offset_(stream->Tell()) {
  stream_->AlignTo(alignment_);
}

CompactDexWriter::ScopedDataSectionItem::~ScopedDataSectionItem() {
  if (deduper_ == nullptr) {
    return;
  }
  // After having written, maybe dedupe the whole section (excluding padding).
  const uint32_t deduped_offset = deduper_->Dedupe(start_offset_,
                                                   stream_->Tell(),
                                                   item_->GetOffset());
  // If we deduped, only use the deduped offset if the alignment matches the required alignment.
  // Otherwise, return without deduping.
  if (deduped_offset != Deduper::kDidNotDedupe && IsAlignedParam(deduped_offset, alignment_)) {
    // Update the IR offset to the offset of the deduped item.
    item_->SetOffset(deduped_offset);
    // Clear the written data for the item so that the stream write doesn't abort in the future.
    stream_->Clear(start_offset_, stream_->Tell() - start_offset_);
    // Since we deduped, restore the offset to the original position.
    stream_->Seek(start_offset_);
  }
}

size_t CompactDexWriter::ScopedDataSectionItem::Written() const {
  return stream_->Tell() - start_offset_;
}

void CompactDexWriter::WriteCodeItem(Stream* stream,
                                     dex_ir::CodeItem* code_item,
                                     bool reserve_only) {
  DCHECK(code_item != nullptr);
  DCHECK(!reserve_only) << "Not supported because of deduping.";
  ScopedDataSectionItem data_item(stream,
                                  code_item,
                                  CompactDexFile::CodeItem::kAlignment,
                                  /* deduper= */ nullptr);

  CompactDexFile::CodeItem disk_code_item;

  uint16_t preheader_storage[CompactDexFile::CodeItem::kMaxPreHeaderSize] = {};
  uint16_t* preheader_end = preheader_storage + CompactDexFile::CodeItem::kMaxPreHeaderSize;
  const uint16_t* preheader = disk_code_item.Create(
      code_item->RegistersSize(),
      code_item->InsSize(),
      code_item->OutsSize(),
      code_item->TriesSize(),
      code_item->InsnsSize(),
      preheader_end);
  const size_t preheader_bytes = (preheader_end - preheader) * sizeof(preheader[0]);

  static constexpr size_t kPayloadInstructionRequiredAlignment = 4;
  const uint32_t current_code_item_start = stream->Tell() + preheader_bytes;
  if (!IsAlignedParam(current_code_item_start, kPayloadInstructionRequiredAlignment) ||
      kIsDebugBuild) {
    // If the preheader is going to make the code unaligned, consider adding 2 bytes of padding
    // before if required.
    IterationRange<DexInstructionIterator> instructions = code_item->Instructions();
    SafeDexInstructionIterator it(instructions.begin(), instructions.end());
    for (; !it.IsErrorState() && it < instructions.end(); ++it) {
      // In case the instruction goes past the end of the code item, make sure to not process it.
      if (std::next(it).IsErrorState()) {
        break;
      }
      const Instruction::Code opcode = it->Opcode();
      // Payload instructions possibly require special alignment for their data.
      if (opcode == Instruction::FILL_ARRAY_DATA ||
          opcode == Instruction::PACKED_SWITCH ||
          opcode == Instruction::SPARSE_SWITCH) {
        stream->Skip(
            RoundUp(current_code_item_start, kPayloadInstructionRequiredAlignment) -
                current_code_item_start);
        break;
      }
    }
  }

  // Write preheader first.
  stream->Write(reinterpret_cast<const uint8_t*>(preheader), preheader_bytes);
  // Registered offset is after the preheader.
  ProcessOffset(stream, code_item);
  // Avoid using sizeof so that we don't write the fake instruction array at the end of the code
  // item.
  stream->Write(&disk_code_item, OFFSETOF_MEMBER(CompactDexFile::CodeItem, insns_));
  // Write the instructions.
  stream->Write(code_item->Insns(), code_item->InsnsSize() * sizeof(uint16_t));
  // Write the post instruction data.
  WriteCodeItemPostInstructionData(stream, code_item, reserve_only);
}

void CompactDexWriter::WriteDebugInfoItem(Stream* stream, dex_ir::DebugInfoItem* debug_info) {
  ScopedDataSectionItem data_item(stream,
                                  debug_info,
                                  SectionAlignment(DexFile::kDexTypeDebugInfoItem),
                                  data_item_dedupe_);
  ProcessOffset(stream, debug_info);
  stream->Write(debug_info->GetDebugInfo(), debug_info->GetDebugInfoSize());
}


CompactDexWriter::Deduper::Deduper(DexContainer::Section* section)
    : dedupe_map_(/*__n=*/ 32,
                  HashedMemoryRange::HashEqual(section),
                  HashedMemoryRange::HashEqual(section)) {}

uint32_t CompactDexWriter::Deduper::Dedupe(uint32_t data_start,
                                           uint32_t data_end,
                                           uint32_t item_offset) {
  HashedMemoryRange range {data_start, data_end - data_start};
  auto existing = dedupe_map_.emplace(range, item_offset);
  if (!existing.second) {
    // Failed to insert means we deduped, return the existing item offset.
    return existing.first->second;
  }
  return kDidNotDedupe;
}

void CompactDexWriter::SortDebugInfosByMethodIndex() {
  static constexpr InvokeType invoke_types[] = {
    kDirect,
    kVirtual
  };
  std::map<const dex_ir::DebugInfoItem*, uint32_t> method_idx_map;
  for (InvokeType invoke_type : invoke_types) {
    for (auto& class_def : header_->ClassDefs()) {
      // Skip classes that are not defined in this dex file.
      dex_ir::ClassData* class_data = class_def->GetClassData();
      if (class_data == nullptr) {
        continue;
      }
      for (auto& method : *(invoke_type == InvokeType::kDirect
                                ? class_data->DirectMethods()
                                : class_data->VirtualMethods())) {
        const dex_ir::MethodId* method_id = method.GetMethodId();
        dex_ir::CodeItem* code_item = method.GetCodeItem();
        if (code_item != nullptr && code_item->DebugInfo() != nullptr) {
          const dex_ir::DebugInfoItem* debug_item = code_item->DebugInfo();
          method_idx_map.insert(std::make_pair(debug_item, method_id->GetIndex()));
        }
      }
    }
  }
  std::sort(header_->DebugInfoItems().begin(),
            header_->DebugInfoItems().end(),
            [&](const std::unique_ptr<dex_ir::DebugInfoItem>& a,
                const std::unique_ptr<dex_ir::DebugInfoItem>& b) {
    auto it_a = method_idx_map.find(a.get());
    auto it_b = method_idx_map.find(b.get());
    uint32_t idx_a = it_a != method_idx_map.end() ? it_a->second : 0u;
    uint32_t idx_b = it_b != method_idx_map.end() ? it_b->second : 0u;
    return idx_a < idx_b;
  });
}

void CompactDexWriter::WriteHeader(Stream* stream) {
  CompactDexFile::Header header;
  CompactDexFile::WriteMagic(&header.magic_[0]);
  CompactDexFile::WriteCurrentVersion(&header.magic_[0]);
  header.checksum_ = header_->Checksum();
  header.signature_ = header_->Signature();
  header.file_size_ = header_->FileSize();
  // Since we are not necessarily outputting the same format as the input, avoid using the stored
  // header size.
  header.header_size_ = GetHeaderSize();
  header.endian_tag_ = header_->EndianTag();
  header.link_size_ = header_->LinkSize();
  header.link_off_ = header_->LinkOffset();
  header.map_off_ = header_->MapListOffset();
  header.string_ids_size_ = header_->StringIds().Size();
  header.string_ids_off_ = header_->StringIds().GetOffset();
  header.type_ids_size_ = header_->TypeIds().Size();
  header.type_ids_off_ = header_->TypeIds().GetOffset();
  header.proto_ids_size_ = header_->ProtoIds().Size();
  header.proto_ids_off_ = header_->ProtoIds().GetOffset();
  header.field_ids_size_ = header_->FieldIds().Size();
  header.field_ids_off_ = header_->FieldIds().GetOffset();
  header.method_ids_size_ = header_->MethodIds().Size();
  header.method_ids_off_ = header_->MethodIds().GetOffset();
  header.class_defs_size_ = header_->ClassDefs().Size();
  header.class_defs_off_ = header_->ClassDefs().GetOffset();
  header.data_size_ = header_->DataSize();
  header.data_off_ = header_->DataOffset();
  header.owned_data_begin_ = owned_data_begin_;
  header.owned_data_end_ = owned_data_end_;

  // Compact dex specific flags.
  header.debug_info_offsets_pos_ = debug_info_offsets_pos_;
  header.debug_info_offsets_table_offset_ = debug_info_offsets_table_offset_;
  header.debug_info_base_ = debug_info_base_;
  header.feature_flags_ = 0u;
  // In cases where apps are converted to cdex during install, maintain feature flags so that
  // the verifier correctly verifies apps that aren't targetting default methods.
  if (header_->SupportDefaultMethods()) {
    header.feature_flags_ |= static_cast<uint32_t>(CompactDexFile::FeatureFlags::kDefaultMethods);
  }
  stream->Seek(0);
  stream->Overwrite(reinterpret_cast<uint8_t*>(&header), sizeof(header));
}

size_t CompactDexWriter::GetHeaderSize() const {
  return sizeof(CompactDexFile::Header);
}

void CompactDexWriter::WriteStringData(Stream* stream, dex_ir::StringData* string_data) {
  ScopedDataSectionItem data_item(stream,
                                  string_data,
                                  SectionAlignment(DexFile::kDexTypeStringDataItem),
                                  data_item_dedupe_);
  ProcessOffset(stream, string_data);
  stream->WriteUleb128(CountModifiedUtf8Chars(string_data->Data()));
  stream->Write(string_data->Data(), strlen(string_data->Data()));
  // Skip null terminator (already zeroed out, no need to write).
  stream->Skip(1);
}

bool CompactDexWriter::CanGenerateCompactDex(std::string* error_msg) {
  static constexpr InvokeType invoke_types[] = {
    kDirect,
    kVirtual
  };
  std::vector<bool> saw_method_id(header_->MethodIds().Size(), false);
  std::vector<dex_ir::CodeItem*> method_id_code_item(header_->MethodIds().Size(), nullptr);
  std::vector<dex_ir::DebugInfoItem*> method_id_debug_info(header_->MethodIds().Size(), nullptr);
  for (InvokeType invoke_type : invoke_types) {
    for (auto& class_def : header_->ClassDefs()) {
      // Skip classes that are not defined in this dex file.
      dex_ir::ClassData* class_data = class_def->GetClassData();
      if (class_data == nullptr) {
        continue;
      }
      for (auto& method : *(invoke_type == InvokeType::kDirect
                                ? class_data->DirectMethods()
                                : class_data->VirtualMethods())) {
        const uint32_t idx = method.GetMethodId()->GetIndex();
        dex_ir::CodeItem* code_item = method.GetCodeItem();
        dex_ir:: DebugInfoItem* debug_info_item = nullptr;
        if (code_item != nullptr) {
          debug_info_item = code_item->DebugInfo();
        }
        if (saw_method_id[idx]) {
          if (method_id_code_item[idx] != code_item) {
            *error_msg = android::base::StringPrintf("Conflicting code item for method id %u",
                                                     idx);
            // Conflicting info, abort generation.
            return false;
          }
          if (method_id_debug_info[idx] != debug_info_item) {
            *error_msg = android::base::StringPrintf("Conflicting debug info for method id %u",
                                                     idx);
            // Conflicting info, abort generation.
            return false;
          }
        }
        method_id_code_item[idx] = code_item;
        method_id_debug_info[idx] = debug_info_item;
        saw_method_id[idx] = true;
      }
    }
  }
  return true;
}

bool CompactDexWriter::Write(DexContainer* output, std::string* error_msg)  {
  DCHECK(error_msg != nullptr);
  CHECK(compute_offsets_);
  CHECK(output->IsCompactDexContainer());

  if (!CanGenerateCompactDex(error_msg)) {
    return false;
  }

  Container* const container = down_cast<Container*>(output);
  // For now, use the same stream for both data and metadata.
  Stream temp_main_stream(output->GetMainSection());
  CHECK_EQ(output->GetMainSection()->Size(), 0u);
  Stream temp_data_stream(output->GetDataSection());
  Stream* main_stream = &temp_main_stream;
  Stream* data_stream = &temp_data_stream;

  // We want offset 0 to be reserved for null, seek to the data section alignment or the end of the
  // section.
  data_stream->Seek(std::max(
      static_cast<uint32_t>(output->GetDataSection()->Size()),
      kDataSectionAlignment));
  data_item_dedupe_ = &container->data_item_dedupe_;

  // Starting offset is right after the header.
  main_stream->Seek(GetHeaderSize());

  // Based on: https://source.android.com/devices/tech/dalvik/dex-format
  // Since the offsets may not be calculated already, the writing must be done in the correct order.
  const uint32_t string_ids_offset = main_stream->Tell();
  WriteStringIds(main_stream, /*reserve_only=*/ true);
  WriteTypeIds(main_stream);
  const uint32_t proto_ids_offset = main_stream->Tell();
  WriteProtoIds(main_stream, /*reserve_only=*/ true);
  WriteFieldIds(main_stream);
  WriteMethodIds(main_stream);
  const uint32_t class_defs_offset = main_stream->Tell();
  WriteClassDefs(main_stream, /*reserve_only=*/ true);
  const uint32_t call_site_ids_offset = main_stream->Tell();
  WriteCallSiteIds(main_stream, /*reserve_only=*/ true);
  WriteMethodHandles(main_stream);

  if (compute_offsets_) {
    // Data section.
    data_stream->AlignTo(kDataSectionAlignment);
  }
  owned_data_begin_ = data_stream->Tell();

  // Write code item first to minimize the space required for encoded methods.
  // For cdex, the code items don't depend on the debug info.
  WriteCodeItems(data_stream, /*reserve_only=*/ false);

  // Sort the debug infos by method index order, this reduces size by ~0.1% by reducing the size of
  // the debug info offset table.
  SortDebugInfosByMethodIndex();
  WriteDebugInfoItems(data_stream);

  WriteEncodedArrays(data_stream);
  WriteAnnotations(data_stream);
  WriteAnnotationSets(data_stream);
  WriteAnnotationSetRefs(data_stream);
  WriteAnnotationsDirectories(data_stream);
  WriteTypeLists(data_stream);
  WriteClassDatas(data_stream);
  WriteStringDatas(data_stream);
  WriteHiddenapiClassData(data_stream);

  // Write delayed id sections that depend on data sections.
  {
    Stream::ScopedSeek seek(main_stream, string_ids_offset);
    WriteStringIds(main_stream, /*reserve_only=*/ false);
  }
  {
    Stream::ScopedSeek seek(main_stream, proto_ids_offset);
    WriteProtoIds(main_stream, /*reserve_only=*/ false);
  }
  {
    Stream::ScopedSeek seek(main_stream, class_defs_offset);
    WriteClassDefs(main_stream, /*reserve_only=*/ false);
  }
  {
    Stream::ScopedSeek seek(main_stream, call_site_ids_offset);
    WriteCallSiteIds(main_stream, /*reserve_only=*/ false);
  }

  // Write the map list.
  if (compute_offsets_) {
    data_stream->AlignTo(SectionAlignment(DexFile::kDexTypeMapList));
    header_->SetMapListOffset(data_stream->Tell());
  } else {
    data_stream->Seek(header_->MapListOffset());
  }

  // Map items are included in the data section.
  GenerateAndWriteMapItems(data_stream);

  // Write link data if it exists.
  const std::vector<uint8_t>& link_data = header_->LinkData();
  if (link_data.size() > 0) {
    CHECK_EQ(header_->LinkSize(), static_cast<uint32_t>(link_data.size()));
    if (compute_offsets_) {
      header_->SetLinkOffset(data_stream->Tell());
    } else {
      data_stream->Seek(header_->LinkOffset());
    }
    data_stream->Write(&link_data[0], link_data.size());
  }

  // Write debug info offset table last to make dex file verifier happy.
  WriteDebugInfoOffsetTable(data_stream);

  data_stream->AlignTo(kDataSectionAlignment);
  owned_data_end_ = data_stream->Tell();
  if (compute_offsets_) {
    header_->SetDataSize(data_stream->Tell());
    if (header_->DataSize() != 0) {
      // Offset must be zero when the size is zero.
      main_stream->AlignTo(kDataSectionAlignment);
      // For now, default to saying the data is right after the main stream.
      header_->SetDataOffset(main_stream->Tell());
    } else {
      header_->SetDataOffset(0u);
    }
  }

  // Write header last.
  if (compute_offsets_) {
    header_->SetFileSize(main_stream->Tell());
  }
  WriteHeader(main_stream);

  // Trim sections to make sure they are sized properly.
  output->GetMainSection()->Resize(header_->FileSize());
  output->GetDataSection()->Resize(data_stream->Tell());

  if (dex_layout_->GetOptions().update_checksum_) {
    // Compute the cdex section (also covers the used part of the data section).
    header_->SetChecksum(CompactDexFile::CalculateChecksum(output->GetMainSection()->Begin(),
                                                           output->GetMainSection()->Size(),
                                                           output->GetDataSection()->Begin(),
                                                           output->GetDataSection()->Size()));
    // Rewrite the header with the calculated checksum.
    WriteHeader(main_stream);
  }

  return true;
}

std::unique_ptr<DexContainer> CompactDexWriter::CreateDexContainer() const {
  return std::unique_ptr<DexContainer>(new CompactDexWriter::Container());
}

}  // namespace art
