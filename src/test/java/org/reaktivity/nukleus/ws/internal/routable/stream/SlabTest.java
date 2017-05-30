/**
 * Copyright 2016-2017 The Reaktivity Project
 *
 * The Reaktivity Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.reaktivity.nukleus.ws.internal.routable.stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.agrona.MutableDirectBuffer;
import org.junit.Test;

public class SlabTest
{
    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectSlotCapacityNotPowerOfTwo()
    {
        new Slab(1024, 100);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectTotalCapacityNotPowerOfTwo()
    {
        new Slab(10000, 1024);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectSlotCapacityGreaterThanTotalCapacity()
    {
        new Slab(256, 512);
    }

    @Test
    public void acquireShouldAllocateSlot() throws Exception
    {
        Slab slab = new Slab(16, 4);
        int slot = slab.acquire(123);
        assertTrue(slot >= 0 && slot < 4);
    }

    @Test
    public void acquireShouldAllocateDifferentSlotsForDifferentStreams() throws Exception
    {
        Slab slab = new Slab(512 * 1024, 1024);
        int slot1 = slab.acquire(111);
        assertTrue(slot1 >= 0);
        int slot2 = slab.acquire(112);
        assertTrue(slot2 >= 0);

        assertNotEquals(slot1, slot2);
    }

    @Test
    public void acquireShouldAllocateDifferentSlotsForDifferentStreamsWithSameHashcode() throws Exception
    {
        Slab slab = new Slab(512 * 1024, 1024);

        int slot1 = slab.acquire(1);
        assertTrue(slot1 >= 0);

        int slot2 = slab.acquire(16);
        assertTrue(slot2 >= 0);

        assertNotEquals(slot1, slot2);
    }

    @Test
    public void acquireShouldReportOutOfMemory() throws Exception
    {
        Slab slab = new Slab(256, 16);
        int slot = 0;
        int i;
        for (i = 0; i < 16; i++)
        {
            int streamId = 111 + i;
            slot = slab.acquire(streamId);
            assertTrue(slot >= 0);
        }
        slot = slab.acquire(111 + i);
        assertEquals(Slab.NO_SLOT, slot);
    }

    @Test
    public void bufferShouldReturnCorrectlySizedBuffer() throws Exception
    {
        Slab slab = new Slab(256, 16);
        int slot = slab.acquire(124123490L);
        MutableDirectBuffer buffer = slab.buffer(slot);
        buffer.putInt(0, 123);
        assertEquals(123, buffer.getInt(0));
        assertEquals(16, buffer.capacity());
    }

    @Test
    public void freeShouldMakeSlotAvailableForReuse() throws Exception
    {
        Slab slab = new Slab(16 * 1024, 1024);
        int slot = 0;
        int i;
        for (i=0; i < 16; i++)
        {
            int streamId = 111 + i;
            slot = slab.acquire(streamId);
            assertTrue(slot >= 0);
        }
        int slotBad = slab.acquire(111 + i);
        assertEquals(Slab.NO_SLOT, slotBad);
        slab.release(slot);
        slot = slab.acquire(111 + i);
        assertNotEquals(Slab.NO_SLOT, slot);
    }

}

