package net.sourceforge.moonstone.android

import net.sourceforge.moonstone.components.impl.AlertDialogComponent
import net.sourceforge.moonstone.components.impl.BadgeComponent
import net.sourceforge.moonstone.components.impl.BottomNavigationComponent
import net.sourceforge.moonstone.components.impl.BottomSheetComponent
import net.sourceforge.moonstone.components.impl.BoxComponent
import net.sourceforge.moonstone.components.impl.ButtonComponent
import net.sourceforge.moonstone.components.impl.CardComponent
import net.sourceforge.moonstone.components.impl.CheckboxComponent
import net.sourceforge.moonstone.components.impl.ChipComponent
import net.sourceforge.moonstone.components.impl.CircleDrawComponent
import net.sourceforge.moonstone.components.impl.ColumnComponent
import net.sourceforge.moonstone.components.impl.DividerComponent
import net.sourceforge.moonstone.components.impl.DynamicListComponent
import net.sourceforge.moonstone.components.impl.ErrorBoundaryComponent
import net.sourceforge.moonstone.components.impl.FloatingActionButtonComponent
import net.sourceforge.moonstone.components.impl.GameCanvasComponent
import net.sourceforge.moonstone.components.impl.IconComponent
import net.sourceforge.moonstone.components.impl.ImageComponent
import net.sourceforge.moonstone.components.impl.LazyColumnComponent
import net.sourceforge.moonstone.components.impl.LazyRowComponent
import net.sourceforge.moonstone.components.impl.LineDrawComponent
import net.sourceforge.moonstone.components.impl.ListItemComponent
import net.sourceforge.moonstone.components.impl.NavItemComponent
import net.sourceforge.moonstone.components.impl.OutlinedTextFieldComponent
import net.sourceforge.moonstone.components.impl.ProgressIndicatorComponent
import net.sourceforge.moonstone.components.impl.RadioButtonComponent
import net.sourceforge.moonstone.components.impl.RectDrawComponent
import net.sourceforge.moonstone.components.impl.RowComponent
import net.sourceforge.moonstone.components.impl.ScaffoldComponent
import net.sourceforge.moonstone.components.impl.SliderComponent
import net.sourceforge.moonstone.components.impl.SnackbarComponent
import net.sourceforge.moonstone.components.impl.SpacerComponent
import net.sourceforge.moonstone.components.impl.SurfaceComponent
import net.sourceforge.moonstone.components.impl.SwitchComponent
import net.sourceforge.moonstone.components.impl.SwitchViewComponent
import net.sourceforge.moonstone.components.impl.TextComponent
import net.sourceforge.moonstone.components.impl.TextFieldComponent
import net.sourceforge.moonstone.components.impl.TopAppBarComponent
import net.sourceforge.moonstone.components.impl.ViewComponent
import net.sourceforge.moonstone.runtime.MoonstoneRuntime

/**
 * Registers all standard components to a MoonstoneRuntime.
 */
fun registerAllComponents(runtime: MoonstoneRuntime) {
    // Register layout components
    runtime.registerComponent(BoxComponent())
    runtime.registerComponent(ColumnComponent())
    runtime.registerComponent(RowComponent())
    runtime.registerComponent(SurfaceComponent())
    runtime.registerComponent(SpacerComponent())

    // Register display components
    runtime.registerComponent(TextComponent())

    // Register interactive components (Phase 3)
    runtime.registerComponent(ButtonComponent())
    runtime.registerComponent(TextFieldComponent())
    runtime.registerComponent(OutlinedTextFieldComponent())
    runtime.registerComponent(CheckboxComponent())
    runtime.registerComponent(SwitchComponent())
    runtime.registerComponent(RadioButtonComponent())

    // Register advanced components (Phase 6)
    // Icon
    runtime.registerComponent(IconComponent())

    // Lists
    runtime.registerComponent(LazyColumnComponent())
    runtime.registerComponent(LazyRowComponent())
    runtime.registerComponent(ListItemComponent())
    runtime.registerComponent(DynamicListComponent())

    // Navigation
    runtime.registerComponent(ScaffoldComponent())
    runtime.registerComponent(TopAppBarComponent())
    runtime.registerComponent(BottomNavigationComponent())
    runtime.registerComponent(NavItemComponent())

    // Dialogs
    runtime.registerComponent(AlertDialogComponent())
    runtime.registerComponent(BottomSheetComponent())
    runtime.registerComponent(SnackbarComponent())

    // Conditional rendering
    runtime.registerComponent(SwitchViewComponent())
    runtime.registerComponent(ViewComponent())

    // Error handling (Phase 7)
    runtime.registerComponent(ErrorBoundaryComponent())

    // New Material 3 components (Phase 8)
    runtime.registerComponent(CardComponent())
    runtime.registerComponent(SliderComponent())
    runtime.registerComponent(ProgressIndicatorComponent())
    runtime.registerComponent(ChipComponent())
    runtime.registerComponent(DividerComponent())
    runtime.registerComponent(ImageComponent())
    runtime.registerComponent(FloatingActionButtonComponent())
    runtime.registerComponent(BadgeComponent())

    // Game components
    runtime.registerComponent(GameCanvasComponent())
    runtime.registerComponent(RectDrawComponent())
    runtime.registerComponent(CircleDrawComponent())
    runtime.registerComponent(LineDrawComponent())
}
