package com.truckerload.domain.feedback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImprovementFeedbackMailTest {

    @Test
    fun compose_blankMessage_returnsNull() {
        assertNull(
            ImprovementFeedbackMail.compose(
                topic = ImprovementFeedbackMail.Topic.ADD,
                message = "   ",
                topicLabel = "Need to add",
                appVersion = "1.0 (1)",
                androidRelease = "14",
            ),
        )
    }

    @Test
    fun compose_buildsSubjectBodyAndSupportAddress() {
        val draft = ImprovementFeedbackMail.compose(
            topic = ImprovementFeedbackMail.Topic.BROKEN,
            message = "Diesel save button does nothing",
            topicLabel = "Does not work",
            appVersion = "1.4.2 (88)",
            androidRelease = "14",
        )!!

        assertEquals("Truckerlogsupport@gmail.com", draft.to)
        assertEquals("[Truck Log] Does not work", draft.subject)
        assertTrue(draft.body.contains("Тема: Does not work"))
        assertTrue(draft.body.contains("Diesel save button does nothing"))
        assertTrue(draft.body.contains("Truck Log 1.4.2 (88)"))
        assertTrue(draft.body.contains("Android 14"))
    }

    @Test
    fun mailtoUri_targetsSupportInboxWithEncodedSubject() {
        val draft = ImprovementFeedbackMail.Draft(
            to = ImprovementFeedbackMail.SUPPORT_EMAIL,
            subject = "[Truck Log] Need to add",
            body = "Please add DEF tracking",
        )
        val uri = ImprovementFeedbackMail.mailtoUriString(draft)
        assertTrue(uri.startsWith("mailto:Truckerlogsupport@gmail.com?"))
        assertTrue(uri.contains("subject="))
        assertTrue(uri.contains("Need%20to%20add") || uri.contains("Need+to+add"))
        assertTrue(uri.contains("body="))
        assertTrue(uri.contains("DEF"))
    }
}
