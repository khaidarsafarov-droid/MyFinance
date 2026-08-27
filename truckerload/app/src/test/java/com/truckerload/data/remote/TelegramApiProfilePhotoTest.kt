package com.truckerload.data.remote

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class TelegramApiProfilePhotoTest {

    @Test
    fun profilePhotoStaticAttachJson_usesMultipartAttachName() {
        val obj = JSONObject(TelegramApi.profilePhotoStaticAttachJson())
        assertEquals("static", obj.getString("type"))
        assertEquals("attach://logo", obj.getString("photo"))
    }

    @Test
    fun setMyNamePayload_matchesBrand() {
        val obj = JSONObject(TelegramApi.setMyNamePayload("TruckoRig"))
        assertEquals("TruckoRig", obj.getString("name"))
    }
}
