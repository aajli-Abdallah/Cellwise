/*
 * Cellwise - offline Sudoku for travel.
 * Copyright (c) 2026 AAJLI Abdallah <ingenieur.aajli@gmail.com> - github.com/aajli-Abdallah - aajli.com
 * All rights reserved.
 */
package com.aajli.cellwise.core

/** A message for the UI: string resource id plus format arguments. */
data class UiMsg(val res: Int, val args: List<Any> = emptyList())
