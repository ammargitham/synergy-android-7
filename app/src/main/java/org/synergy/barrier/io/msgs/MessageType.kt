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
package org.synergy.barrier.io.msgs

/**
 * For more information on the Synergy message format, see
 * http://synergy-foss.org/code/filedetails.php?repname=synergy&path=%2Ftrunk%2Fsrc%2Flib%2Fsynergy%2FProtocolTypes.cpp
 */
enum class MessageType(val value: String, private val commonName: String) {
    HELLO(
        "Barrier",
        "[Init] Hello"
    ),  // Not a standard message
    HELLOBACK(
        "Barrier",
        "[Init] Hello Back"
    ),  // Not a standard message
    CNOOP(
        "CNOP",
        "[Command] NoOp"
    ),
    CCLOSE(
        "CBYE",
        "[Command] Close"
    ),
    CENTER(
        "CINN",
        "[Command] Enter"
    ),
    CLEAVE(
        "COUT",
        "[Command] Leave"
    ),
    CCLIPBOARD(
        "CCLP",
        "[Command] Clipboard"
    ),
    CSCREENSAVER(
        "CSEC",
        "[Command] Screensaver"
    ),
    CRESETOPTIONS(
        "CROP",
        "[Command] Reset Options"
    ),
    CINFOACK(
        "CIAK",
        "[Command] Info Ack"
    ),
    CKEEPALIVE(
        "CALV",
        "[Command] Keep Alive"
    ),
    DKEYDOWN(
        "DKDN",
        "[Data] Key Down"
    ),
    DKEYREPEAT(
        "DKRP",
        "[Data] Key Repeat"
    ),
    DKEYUP(
        "DKUP",
        "[Data] Key Up"
    ),
    DMOUSEDOWN(
        "DMDN",
        "[Data] Mouse Down"
    ),
    DMOUSEUP(
        "DMUP",
        "[Data] Mouse Up"
    ),
    DMOUSEMOVE(
        "DMMV",
        "[Data] Mouse Move"
    ),
    DMOUSERELMOVE(
        "DMRM",
        "[Data] Mouse Relative Move"
    ),
    DMOUSEWHEEL(
        "DMWM",
        "[Data] Mouse Wheel"
    ),
    DCLIPBOARD(
        "DCLP",
        "[Data] Clipboard"
    ),
    DINFO(
        "DINF",
        "[Data] Info"
    ),
    DSETOPTIONS(
        "DSOP",
        "[Data] Set Options"
    ),
    QINFO(
        "QINF",
        "[Query] Info"
    ),
    EINCOMPATIBLE(
        "EICV",
        "[Error] Incompatible"
    ),
    EBUSY(
        "EBSY",
        "[Error] Busy"
    ),
    EUNKNOWN(
        "EUNK",
        "[Error] Unknown"
    ),
    EBAD(
        "EBAD",
        "[Error] Bad"
    );

    override fun toString() = commonName

    companion object {
        fun fromString(messageValue: String) = values().find {
            messageValue.equals(it.value, ignoreCase = true)
        } ?: throw IllegalArgumentException("No MessageType with value $messageValue")
    }
}