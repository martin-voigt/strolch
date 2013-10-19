/*
 * Copyright (c) 2012, Robert von Burg
 *
 * All rights reserved.
 *
 * This file is part of the XXX.
 *
 *  XXX is free software: you can redistribute 
 *  it and/or modify it under the terms of the GNU General Public License as 
 *  published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  XXX is distributed in the hope that it will 
 *  be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with XXX.  If not, see 
 *  <http://www.gnu.org/licenses/>.
 */
package ch.eitchnet.xmlpers.api;

/**
 * @author Robert von Burg <eitch@eitchnet.ch>
 * 
 */
public enum IoMode {

	DOM {
		@Override
		public <T> void write(PersistenceContext<T> ctx, FileIo fileIo) {
			fileIo.writeDom(ctx);
		}

		@Override
		public <T> void read(PersistenceContext<T> ctx, FileIo fileIo) {
			fileIo.readDom(ctx);
		}
	},
	SAX {
		@Override
		public <T> void write(PersistenceContext<T> ctx, FileIo fileIo) {
			fileIo.writeSax(ctx);
		}

		@Override
		public <T> void read(PersistenceContext<T> ctx, FileIo fileIo) {
			fileIo.readSax(ctx);
		}
	};

	/**
	 * @param ctx
	 * @param fileIo
	 */
	public <T> void write(PersistenceContext<T> ctx, FileIo fileIo) {
		throw new UnsupportedOperationException("Override me!"); //$NON-NLS-1$
	}

	/**
	 * @param ctx
	 * @param fileIo
	 */
	public <T> void read(PersistenceContext<T> ctx, FileIo fileIo) {
		throw new UnsupportedOperationException("Override me!"); //$NON-NLS-1$
	}
}
