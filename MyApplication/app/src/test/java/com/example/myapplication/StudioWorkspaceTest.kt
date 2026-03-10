package com.example.myapplication

import org.junit.Assert.assertEquals
import org.junit.Test

class StudioWorkspaceTest {
    @Test
    fun fromStoredRoute_returns_chat_for_unknown_route() {
        assertEquals(StudioWorkspace.Chat, StudioWorkspace.fromStoredRoute("unknown"))
    }

    @Test
    fun fromStoredRoute_returns_matching_workspace_when_route_is_known() {
        assertEquals(StudioWorkspace.ToolRegistry, StudioWorkspace.fromStoredRoute(StudioWorkspace.ToolRegistry.route))
    }
}