/*
 * Copyright 2014 Robert von Burg <eitch@eitchnet.ch>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package li.strolch.communication;

import java.util.List;

public interface IoMessageArchive {

	public int getMaxSize();

	public void setMaxSize(int maxSize);

	public int getTrimSize();

	public void setTrimSize(int trimSize);

	public int size();

	public List<IoMessage> getAll();

	public List<IoMessage> getBy(String connectionId);

	public List<IoMessage> getBy(String connectionId, CommandKey key);

	public void clearArchive();

	public void archive(IoMessage message);
}
