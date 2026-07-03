<style>
body {
font-family: "Inter", "Inter Regular", "Segoe UI", sans-serif !important;
color: #E6DCD1;
background-color: #191A1C;
line-height: 1.6;
padding: 30px;
max-width: 800px;
}

h1 { color: #648fff }
h2 { color: #fe6100 } 
h3 { color: #dc267f }
h4 { color: #ffb000 }
h5 { color: #785ef0 }

code, pre, pre code {
font-family: "Google Sans Code", "Consolas", monospace !important;
color: #009E30;
font-weight: 700 !important;
background-color: #191A1C;
}

pre {
padding: 12px;
overflow-x: auto;
display: block;
}

</style>

# Code Maintenance Notes

## Icons

To add an `.svg` icon, first ensure the following is added to the FXML file: `<?import javafx.scene.shape.SVGPath?>`.

Then, under the element you want to add the icon:

```xml
<graphic>
    <SVGPath content="PASTE-SVG-CONTENT-HERE" styleClass="name-of-style-in-css"/>
</graphic>
```

Where you see `"PASTE-SVG-CONTENT-HERE"` replace this with the content of the SVG you wish to add. Open the SVG file and navigate to the `path` tag. Some SVG will have multiple path tags, like an anchor tag, for example:

```xml
<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" aria-hidden="true" role="img" width="64" height="64" viewBox="0 0 24 24" style="color: rgb(74, 85, 101);"><g fill="none" fill-rule="evenodd"><path d="m12.593 23.258l-.011.002l-.071.035l-.02.004l-.014-.004l-.071-.035q-.016-.005-.024.005l-.004.01l-.017.428l.005.02l.01.013l.104.074l.015.004l.012-.004l.104-.074l.012-.016l.004-.017l-.017-.427q-.004-.016-.017-.018m.265-.113l-.013.002l-.185.093l-.01.01l-.003.011l.018.43l.005.012l.008.007l.201.093q.019.005.029-.008l.004-.014l-.034-.614q-.005-.018-.02-.022m-.715.002a.02.02 0 0 0-.027.006l-.006.014l-.034.614q.001.018.017.024l.015-.002l.201-.093l.01-.008l.004-.011l.017-.43l-.003-.012l-.01-.01z"></path><path fill="currentColor" d="M2.5 12A1.5 1.5 0 0 1 4 10.5h16a1.5 1.5 0 0 1 0 3H4A1.5 1.5 0 0 1 2.5 12"></path></g></svg>
```

Notice there are two path tags:

```xml
<!-- tag 1 -->
<path d="m12.593 23.258l-.011.002l-.071.035l-.02.004l-.014-.004l-.071-.035q-.016-.005-.024.005l-.004.01l-.017.428l.005.02l.01.013l.104.074l.015.004l.012-.004l.104-.074l.012-.016l.004-.017l-.017-.427q-.004-.016-.017-.018m.265-.113l-.013.002l-.185.093l-.01.01l-.003.011l.018.43l.005.012l.008.007l.201.093q.019.005.029-.008l.004-.014l-.034-.614q-.005-.018-.02-.022m-.715.002a.02.02 0 0 0-.027.006l-.006.014l-.034.614q.001.018.017.024l.015-.002l.201-.093l.01-.008l.004-.011l.017-.43l-.003-.012l-.01-.01z">

<!-- tag 2 -->
<path fill="currentColor" d="M2.5 12A1.5 1.5 0 0 1 4 10.5h16a1.5 1.5 0 0 1 0 3H4A1.5 1.5 0 0 1 2.5 12"></path>
```

The second path tag is the one you want. Copy and paste the data `d="..."` between the apostrophes of `<SVGPath content=""` in the FXML file. For example:

```xml
<SVGPath content="M2.5 12A1.5 1.5 0 0 1 4 10.5h16a1.5 1.5 0 0 1 0 3H4A1.5 1.5 0 0 1 2.5 12" styleClass="my-style-class"/>
```

Ensure you the `game.css` file contains the `styleClass` you have selected, or create it, if it does not yet exist.