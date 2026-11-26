/*
 * Display Hot Keys
 * Copyright © 2024-2026 Jonathan R. Miller
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of version 3 of the GNU
 * General Public License as published by the Free Software Foundation, together with the additional terms under section
 * 7 of that License in ADDITIONAL_TERMS.txt.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License and the additional terms along with this program in
 * LICENSE.txt and ADDITIONAL_TERMS.txt. If not, see <https://www.gnu.org/licenses/>.
 */
#pragma once

#include <Windows.h>
#include <string>

/**
 * A display searched for by its GDI device name while the attached displays are enumerated, which holds the display's
 * handle and information once it is found.
 */
struct DisplayLookup {
    /**
     * The GDI device name of the display to find.
     */
    std::wstring name;

    /**
     * Handle of the display with that name, or NULL while no attached display has matched it.
     */
    HMONITOR monitor;

    /**
     * Bounds, working area, and device name of the matched display, filled in when it is found.
     */
    MONITORINFOEXW displayInfo;
};
