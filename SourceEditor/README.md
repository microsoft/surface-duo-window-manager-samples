---
page_type: sample
name: "Android foldable SourceEditor"
description: "Uses the dual view layout pattern and external storage capabilities to create a source-editing app for the foldable and large screen devices."
languages:
- kotlin
products:
- surface-duo
urlFragment: source-editor
---

# SourceEditor (with OpenAI 💡)

This sample contains a Kotlin application designed for foldable Android devices. The application is an HTML editor that enables real-time rendering of formatted source code. Making use of the [dual view](https://docs.microsoft.com/dual-screen/introduction#dual-view) app pattern, users are able to edit and preview any changes simultaneously without switching windows.

## Incorporating OpenAI

The idea to do text processing with OpenAI comes from Syncfusion's blog post
on [integrating ChatGPT with Blazor editor in .NET](https://www.syncfusion.com/blogs/post/integrate-chatgpt-blazor-rich-text-editor.aspx)
and [SyncfusionExamples GitHub repo](https://github.com/SyncfusionExamples/Integrating-OpenAI-with-Blazor-RichTextEditor).

The first pass of porting the REST API code from C# to Kotlin was itself suggested by ChatGPT.

> **NOTE:** in **Constants.kt** add your [OpenAI](https://platform.openai.com/docs/api-reference) key in the field `OPENAI_KEY`

## Features

This project makes use of Jetpack [Window Manager](https://developer.android.com/jetpack/androidx/releases/window) to handle events that require consideration of a hinge or fold in the device's screen.

This project also uses [Fragments](https://developer.android.com/guide/components/fragments), one to display a code editing window and another to render and display formatted code through a [WebView](https://developer.android.com/reference/android/webkit/WebView). The app starts in single screen mode, but can be spanned to enable dual screen mode. See the Getting Started section above for more information about spanning.

### Mirrored Scrolling

If more content exists than can be displayed on the screen at once, users can scroll to view hidden content. In dual screen mode, scrolling of one screen (either editor or preview) will be mirrored on the other screen to ensure the user is always looking at relevant content.

### File Saving

To save any changes made in the editor, the save icon in the top toolbar can be pressed. The user will then be brought to a prompt where they can enter a title and location.

### File Selection

To choose a new file to open, the folder icon in the top toolbar can be pressed. A prompt will then open where the user can choose which file they want to select.

### Drag and Drop

This project also supports drag and drop functionality. If a file gallery is open in one screen, text files can be dragged into the Source Editor application. Dropping a file onto the editor or preview screen will import the file's text into the application.

## Icons

SVG for icons from [material-design-icons](https://github.com/google/material-design-icons/blob/master/android/action/lightbulb/materialicons/black/res/drawable/baseline_lightbulb_24.xml).

## Contributing

This project welcomes contributions and suggestions.  Most contributions require you to agree to a
Contributor License Agreement (CLA) declaring that you have the right to, and actually do, grant us
the rights to use your contribution. For details, visit https://cla.opensource.microsoft.com.

When you submit a pull request, a CLA bot will automatically determine whether you need to provide
a CLA and decorate the PR appropriately (e.g., status check, comment). Simply follow the instructions
provided by the bot. You will only need to do this once across all repos using our CLA.

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/).
For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or
contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.

## License

Copyright (c) Microsoft Corporation.

MIT License

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED AS IS, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
