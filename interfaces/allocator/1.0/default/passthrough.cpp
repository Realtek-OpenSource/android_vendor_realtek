/*
 * Copyright 2016 The Android Open Source Project
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

#include <allocator-passthrough/1.0/GrallocLoader.h>
#include <vendor/realtek/allocator/1.0/IAllocator.h>

using vendor::realtek::allocator::V1_0::IAllocator;
using vendor::realtek::allocator::V1_0::passthrough::GrallocLoader;

extern "C" IAllocator* HIDL_FETCH_IAllocator(const char* /* name */) {
    return GrallocLoader::load();
}
