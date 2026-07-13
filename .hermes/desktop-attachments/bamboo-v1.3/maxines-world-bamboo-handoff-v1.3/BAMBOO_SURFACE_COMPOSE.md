# Compose Assembly Reference for BambooSurface

This is an implementation contract, not a drop-in library. Adapt package names and theme tokens to the repository.

```kotlin
@Composable
fun BambooSurface(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    railThickness: Dp = 12.dp,
    cornerSize: Dp = 22.dp,
    subjectAccent: Color? = null,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .semantics(mergeDescendants = true) { disabled = !enabled }
    ) {
        Image(
            painter = painterResource(R.drawable.fill_sawali),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize().clip(RoundedCornerShape(18.dp))
        )

        Image(
            painter = painterResource(R.drawable.rail_bamboo_horizontal),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth().height(railThickness).align(Alignment.TopCenter)
        )
        Image(
            painter = painterResource(R.drawable.rail_bamboo_horizontal),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth().height(railThickness).align(Alignment.BottomCenter)
        )
        Image(
            painter = painterResource(R.drawable.rail_bamboo_vertical),
            contentDescription = null,
            contentScale = ContentScale.FillHeight,
            modifier = Modifier.fillMaxHeight().width(railThickness).align(Alignment.CenterStart)
        )
        Image(
            painter = painterResource(R.drawable.rail_bamboo_vertical),
            contentDescription = null,
            contentScale = ContentScale.FillHeight,
            modifier = Modifier.fillMaxHeight().width(railThickness).align(Alignment.CenterEnd)
        )

        BambooCorner(R.drawable.corner_rattan_tl, Alignment.TopStart, cornerSize)
        BambooCorner(R.drawable.corner_rattan_tr, Alignment.TopEnd, cornerSize)
        BambooCorner(R.drawable.corner_rattan_bl, Alignment.BottomStart, cornerSize)
        BambooCorner(R.drawable.corner_rattan_br, Alignment.BottomEnd, cornerSize)

        if (subjectAccent != null) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(4.dp)
                    .padding(horizontal = cornerSize)
                    .background(subjectAccent)
            )
        }

        Box(Modifier.matchParentSize().padding(contentPadding), content = content)
    }
}
```

Do not use this sample unchanged if `FillWidth` or `FillHeight` visibly distorts bamboo joints. In that case, tile the middle rail segment with `drawWithCache` while preserving fixed rail thickness. The final rendered result, not the shortest code, is authoritative.
