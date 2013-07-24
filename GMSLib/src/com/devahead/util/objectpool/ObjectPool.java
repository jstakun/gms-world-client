/*
 * Copyright (c) 2011, Andrea Bresolin (http://www.devahead.com)
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer;
 * - redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * - neither the name of www.devahead.com nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.devahead.util.objectpool;

/**
 * Object pool implementation.
 * 
 * @author Andrea Bresolin
 */
public class ObjectPool
{
	protected final int MAX_FREE_OBJECT_INDEX;

	protected PoolObjectFactory factory;
	protected PoolObject[] freeObjects;
	protected int freeObjectIndex = -1;

	/**
	 * Constructor.
	 * 
	 * @param factory the object pool factory instance
	 * @param maxSize the maximun number of instances stored in the pool
	 */
	public ObjectPool(PoolObjectFactory factory, int maxSize)
	{
		this.factory = factory;
		this.freeObjects = new PoolObject[maxSize];
		MAX_FREE_OBJECT_INDEX = maxSize - 1;
	}
	
	/**
	 * Creates a new object or returns a free object from the pool.
	 * 
	 * @return a PoolObject instance already initialized
	 */
	public synchronized PoolObject newObject()
	{
		PoolObject obj;
		
		if (freeObjectIndex == -1)
		{
			// There are no free objects so I just
			// create a new object that is not in the pool.
			obj = factory.createPoolObject();
		}
		else
		{
			// Get an object from the pool
			obj = freeObjects[freeObjectIndex];
			
			freeObjectIndex--;
		}

		// Initialize the object
		obj.initializePoolObject();
		
		return obj;
	}

	/**
	 * Stores an object instance in the pool to make it available for a subsequent
	 * call to newObject() (the object is considered free).
	 * 
	 * @param obj the object to store in the pool and that will be finalized
	 */
	public synchronized void freeObject(PoolObject obj)
	{
		if (obj != null)
		{
			// Finalize the object
			obj.finalizePoolObject();
			
			// I can put an object in the pool only if there is still room for it
			if (freeObjectIndex < MAX_FREE_OBJECT_INDEX)
			{
				freeObjectIndex++;
				
				// Put the object in the pool
				freeObjects[freeObjectIndex] = obj;
			}
		}
	}
}
