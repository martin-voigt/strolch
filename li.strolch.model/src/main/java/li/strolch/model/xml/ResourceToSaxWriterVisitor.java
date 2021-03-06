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
package li.strolch.model.xml;

import java.text.MessageFormat;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import li.strolch.exception.StrolchException;
import li.strolch.model.Resource;
import li.strolch.model.visitor.ResourceVisitor;

/**
 * @author Robert von Burg <eitch@eitchnet.ch>
 */
public class ResourceToSaxWriterVisitor extends StrolchElementToSaxWriterVisitor implements ResourceVisitor<Void> {

	public ResourceToSaxWriterVisitor(XMLStreamWriter writer) {
		super(writer);
	}

	@Override
	public Void visit(Resource resource) {
		try {
			writeElement(resource);
			this.writer.flush();
		} catch (XMLStreamException e) {
			String msg = "Failed to write Resource {0} due to {1}"; //$NON-NLS-1$
			msg = MessageFormat.format(msg, resource.getLocator(), e.getMessage());
			throw new StrolchException(msg, e);
		}

		return null;
	}
}
