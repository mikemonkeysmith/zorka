/** 
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * 
 * ZORKA is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * ZORKA is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * ZORKA. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.agent;

import java.io.Closeable;
import java.io.IOException;


public class ZorkaBshWorker implements Runnable, Closeable {

    // TODO integrate with ZorkaCallback, create single interface representing zorka query execution task;

	private final ZorkaBshAgent agent;
	private final String expr;
	private final ZorkaCallback callback;
	
	public ZorkaBshWorker(ZorkaBshAgent agent, String expr, ZorkaCallback callback) {
		this.agent = agent;
		this.expr = expr;
		this.callback = callback;
	}
	
	public void run() {
		try {
			callback.handleResult(agent.eval(expr));
		} catch (Throwable e) {
			callback.handleError(e);
		} 
	}

    public void close() throws IOException {
        callback.handleError(new RuntimeException("Request timed out."));
    }
}
