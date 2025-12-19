package com.augmentalis.magicelements.renderer.ios.mappers

import com.augmentalis.magicelements.components.phase3.onboarding.IntroScreen
import com.augmentalis.magicelements.components.phase3.onboarding.IntroPage
import com.augmentalis.magicelements.renderer.ios.bridge.*

object IntroScreenMapper {
    fun map(component: IntroScreen): SwiftUIComponent {
        val pageViews = component.pages.mapIndexed { index, page ->
            SwiftUIComponent(
                type = "VStack",
                props = mapOf("spacing" to 24, "alignment" to "center"),
                children = listOfNotNull(
                    page.imageUrl?.let {
                        SwiftUIComponent(type = "AsyncImage", props = mapOf("url" to it, "width" to 200, "height" to 200))
                    },
                    page.icon?.let {
                        SwiftUIComponent(type = "Image", props = mapOf("systemName" to it, "fontSize" to 80))
                    },
                    SwiftUIComponent(type = "Text", props = mapOf("content" to page.title, "font" to "title", "fontWeight" to "bold")),
                    SwiftUIComponent(type = "Text", props = mapOf("content" to page.description, "font" to "body", "foregroundColor" to "secondary", "multilineTextAlignment" to "center"))
                )
            )
        }

        return SwiftUIComponent(
            type = "TabView",
            props = mapOf(
                "selection" to component.currentPage,
                "tabViewStyle" to "page",
                "indexDisplayMode" to "always"
            ),
            children = pageViews + listOf(
                SwiftUIComponent(
                    type = "HStack",
                    children = listOfNotNull(
                        if (component.showSkip) SwiftUIComponent(type = "Button", props = mapOf("label" to component.skipLabel, "style" to "plain")) else null,
                        SwiftUIComponent(type = "Spacer"),
                        if (component.currentPage < component.pages.size - 1 && component.showNext)
                            SwiftUIComponent(type = "Button", props = mapOf("label" to component.nextLabel, "style" to "bordered"))
                        else if (component.showDone)
                            SwiftUIComponent(type = "Button", props = mapOf("label" to component.doneLabel, "style" to "borderedProminent"))
                        else null
                    )
                )
            )
        )
    }
}
