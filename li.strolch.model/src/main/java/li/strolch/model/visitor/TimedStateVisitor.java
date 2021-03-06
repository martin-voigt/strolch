/*
 * Copyright 2013 Robert von Burg <eitch@eitchnet.ch>
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
package li.strolch.model.visitor;

import li.strolch.model.timedstate.BooleanTimedState;
import li.strolch.model.timedstate.FloatTimedState;
import li.strolch.model.timedstate.IntegerTimedState;
import li.strolch.model.timedstate.StringSetTimedState;
import li.strolch.model.timedstate.StrolchTimedState;

/**
 * @author Robert von Burg <eitch@eitchnet.ch>
 */
public interface TimedStateVisitor {

	public <T> T visitTimedState(StrolchTimedState<?> timedState);

	public <T> T visitBooleanState(BooleanTimedState state);

	public <T> T visitFloatState(FloatTimedState state);

	public <T> T visitIntegerState(IntegerTimedState state);

	public <T> T visitStringState(StringSetTimedState state);

}
