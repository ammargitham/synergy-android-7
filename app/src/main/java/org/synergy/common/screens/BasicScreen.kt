/*
 * synergy -- mouse and keyboard sharing utility
 * Copyright (C) 2010 Shaun Patterson
 * Copyright (C) 2010 The Synergy Project
 * Copyright (C) 2009 The Synergy+ Project
 * Copyright (C) 2002 Chris Schoeneman
 *
 * This package is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * found in the file COPYING that should have accompanied this file.
 *
 * This package is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.synergy.common.screens

import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import org.synergy.base.utils.Log
import org.synergy.services.BarrierAccessibilityAction.*
import java.util.*


class BasicScreen(private val context: Context) : ScreenInterface {
    private val buttonToKeyDownID: IntArray = IntArray(256)

    // Keep track of the mouse cursor since I cannot find a way of
    //  determining the current mouse position
    private var mouseX = -1
    private var mouseY = -1

    // Screen dimensions
    private var width = 0
    private var height = 0

    /**
     * Set the shape of the screen -- set from the initializing activity
     *
     * @param width
     * @param height
     */
    fun setShape(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    override fun getShape(): Rect {
        return Rect(0, 0, width, height)
    }

    override fun enable() {}

    override fun disable() {}

    override fun enter(toggleMask: Int) {
        allKeysUp()
        context.sendBroadcast(MouseEnter().getIntent())
    }

    override fun leave(): Boolean {
        allKeysUp()
        context.sendBroadcast(MouseLeave().getIntent())
        return true
    }

    private fun allKeysUp() {
        // TODO Auto-generated method stub
    }

    override fun keyDown(id: Int, mask: Int, button: Int) {
        // 1) 'button - 1' appears to be the low-level keyboard scan code
        // 2) 'id' does not appear to be conserved between server keyDown
        // and keyUp event broadcasts as the 'id' on *most* keyUp events
        // appears to be set to 0.  'button' does appear to be conserved
        // so we store the keyDown 'id' using this event so that we can
        // pull out the 'id' used for keyDown for proper keyUp handling
        if (button < buttonToKeyDownID.size) {
            buttonToKeyDownID[button] = id
        } else {
            Log.note("found keyDown button parameter > " + buttonToKeyDownID.size + ", may not be able to properly handle keyUp event.")
        }
        // TODO simulate keydown press
    }

    override fun keyUp(id: Int, mask: Int, button: Int) {
        var id1 = id
        if (button < buttonToKeyDownID.size) {
            val keyDownID = buttonToKeyDownID[button]
            if (keyDownID > -1) {
                id1 = keyDownID
            }
        } else {
            Log.note("found keyUp button parameter > " + buttonToKeyDownID.size + ", may not be able to properly handle keyUp event.")
        }
        // TODO simulate keyup event
    }

    override fun keyRepeat(keyEventID: Int, mask: Int, button: Int) {}

    override fun mouseDown(buttonID: Int) {
        // todo simulate mouse button down? event
    }

    override fun mouseUp(buttonID: Int) {
        // todo simulate mouse button up? event
    }

    override fun mouseMove(x: Int, y: Int) {
        Log.debug("mouseMove: $x, $y")

        // this state appears to signal a screen exit, use this to
        // flag mouse position reinitialization for next call
        // to this method.
        if (x == width && y == height) {
            clearMousePosition(true)
            return
        }
        if (mouseX < 0 || mouseY < 0) {
            mouseX = x
            mouseY = y
        } else {
            val dx = x - mouseX
            val dy = y - mouseY
            // Adjust 'known' cursor position
            mouseX += dx
            mouseY += dy
        }

        context.sendBroadcast(MouseMove(mouseX, mouseY).getIntent())
    }

    override fun mouseRelativeMove(x: Int, y: Int) {
        //Injection.movemouse(x, y);
    }

    override fun mouseWheel(x: Int, y: Int) {
        //Injection.mousewheel(x, y);
    }

    private fun clearMousePosition(inject: Boolean) {
        mouseX = -1
        mouseY = -1
        if (inject) {
            // moving to height/width will hide mouse pointer
            //  Injection.movemouse(width, height);
        }
    }

    override fun getCursorPos(): Point {
        return Point(0, 0)
    }

    override fun getEventTarget(): Any {
        return this
    }

    init {
        // the keyUp/Down/Repeat button parameter appears to be the low-level
        // keyboard scan code (*shouldn't be* more than 256 of these, but I speak
        // from anecdotal experience, not as an expert...
        Arrays.fill(buttonToKeyDownID, -1)
    }
}