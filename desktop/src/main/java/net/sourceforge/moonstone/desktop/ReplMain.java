/*
 * Moonstone REPL Main
 *
 * A development/debug entry point that extends KleinLisp's Main class,
 * enabling interactive development via Emacs/geiser or any Scheme REPL.
 *
 * Usage:
 *   1. Start the REPL: java -cp ... net.sourceforge.moonstone.desktop.ReplMain
 *   2. Load your app script: (load "samples/counter/app.scm")
 *   3. Run the app: (run-app app)
 *   4. Interact with state from the REPL: (state-set! counter 42)
 *   5. See changes reflected in the UI immediately!
 */
package net.sourceforge.moonstone.desktop;

import androidx.compose.runtime.MutableState;

import net.sourceforge.moonstone.components.ComponentFactory;
import net.sourceforge.moonstone.components.ComponentRegistry;
import net.sourceforge.moonstone.components.UIElement;
import net.sourceforge.moonstone.components.UIElementWrapper;
import net.sourceforge.moonstone.components.impl.*;
import net.sourceforge.moonstone.runtime.DerivedStateCell;
import net.sourceforge.moonstone.runtime.Platform;
import net.sourceforge.moonstone.runtime.StateCell;
import net.sourceforge.moonstone.runtime.StateManager;
import net.sourceforge.kleinlisp.Lisp;
import net.sourceforge.kleinlisp.LispEnvironment;
import net.sourceforge.kleinlisp.LispObject;
import net.sourceforge.kleinlisp.Main;
import net.sourceforge.kleinlisp.objects.BooleanObject;
import net.sourceforge.kleinlisp.objects.IntObject;
import net.sourceforge.kleinlisp.objects.JavaObject;
import net.sourceforge.kleinlisp.objects.StringObject;
import net.sourceforge.kleinlisp.objects.VoidObject;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * REPL-based entry point for Moonstone development.
 *
 * This class extends KleinLisp's Main class to provide an interactive
 * development environment where you can:
 * - Load and run GUI applications from the REPL
 * - Inspect and modify application state in real-time
 * - See UI changes immediately without restarting
 *
 * Particularly useful for development with Emacs/geiser or other
 * Scheme IDE integrations.
 */
public class ReplMain extends Main {

    private StateManager stateManager;
    private ComponentRegistry componentRegistry;
    private MutableState<UIElement> rootElement;
    private LispEnvironment env;
    private volatile boolean appRunning = false;
    private Thread uiThread;
    private volatile net.sourceforge.kleinlisp.Function entryPointFunction;

    public static void main(String[] args) throws Exception {
        new ReplMain().run(args);
    }

    @Override
    protected void configureLisp(Lisp lisp) {
        super.configureLisp(lisp);

        this.env = lisp.environment();
        this.stateManager = new StateManager();
        this.componentRegistry = new ComponentRegistry();

        // Register all GUI components
        registerAllComponents();

        // Register state management functions
        registerStateFunctions();

        // Register desktop extensions (toast, vibrate stubs, etc.)
        registerDesktopExtensions();

        // Register the run-app function
        registerRunAppFunction();

        // Register utility functions
        registerUtilityFunctions();
    }

    @Override
    protected String getBanner() {
        return "Moonstone REPL\n" +
               "Interactive Scheme UI development environment.\n" +
               "Type expressions to evaluate. Press Ctrl+D to exit.\n\n" +
               "Quick start:\n" +
               "  1. (load \"samples/counter/app.scm\")\n" +
               "  2. (run-app app)\n" +
               "  3. Modify state from REPL and see changes live!";
    }

    private void registerAllComponents() {
        // Layout components
        registerComponent(new BoxComponent());
        registerComponent(new ColumnComponent());
        registerComponent(new RowComponent());
        registerComponent(new SurfaceComponent());
        registerComponent(new SpacerComponent());

        // Display components
        registerComponent(new TextComponent());

        // Interactive components
        registerComponent(new ButtonComponent());
        registerComponent(new TextFieldComponent());
        registerComponent(new OutlinedTextFieldComponent());
        registerComponent(new CheckboxComponent());
        registerComponent(new SwitchComponent());
        registerComponent(new RadioButtonComponent());

        // Advanced components
        registerComponent(new IconComponent());
        registerComponent(new LazyColumnComponent());
        registerComponent(new LazyRowComponent());
        registerComponent(new ListItemComponent());
        registerComponent(new DynamicListComponent());
        registerComponent(new ScaffoldComponent());
        registerComponent(new TopAppBarComponent());
        registerComponent(new BottomNavigationComponent());
        registerComponent(new NavItemComponent());
        registerComponent(new AlertDialogComponent());
        registerComponent(new BottomSheetComponent());
        registerComponent(new SnackbarComponent());
        registerComponent(new SwitchViewComponent());
        registerComponent(new ViewComponent());
        registerComponent(new ErrorBoundaryComponent());
    }

    private void registerComponent(ComponentFactory factory) {
        componentRegistry.register(factory.getName(), factory);
        env.registerFunction(factory.getName(), params -> factory.create(params));
    }

    private void registerStateFunctions() {
        // state - Create a new state cell with initial value
        env.registerFunction("state", params -> {
            LispObject initialValue = params.length > 0 ? params[0] : VoidObject.VOID;
            return new JavaObject(stateManager.createCell(initialValue));
        });

        // state-ref - Read current value from state cell or derived cell
        env.registerFunction("state-ref", params -> {
            if (params.length == 0) {
                throw new IllegalArgumentException("state-ref requires a state cell argument");
            }

            StateCell cell = params[0].asObject(StateCell.class);
            if (cell != null) {
                return cell.getValue();
            }

            DerivedStateCell derived = params[0].asObject(DerivedStateCell.class);
            if (derived != null) {
                return derived.getValue();
            }

            throw new IllegalArgumentException("state-ref expects a state cell or derived cell");
        });

        // derived - Create a derived/computed state cell
        env.registerFunction("derived", params -> {
            if (params.length == 0) {
                throw new IllegalArgumentException("derived requires a computation function");
            }
            var fn = params[0].asFunction();
            if (fn == null) {
                throw new IllegalArgumentException("derived expects a function argument");
            }
            return new JavaObject(new DerivedStateCell(fn.function()));
        });

        // state-set! - Set new value to state cell
        env.registerFunction("state-set!", params -> {
            if (params.length < 2) {
                throw new IllegalArgumentException("state-set! requires state cell and new value");
            }
            StateCell cell = params[0].asObject(StateCell.class);
            if (cell == null) {
                throw new IllegalArgumentException("First argument must be a state cell");
            }
            cell.setValue(params[1]);
            return VoidObject.VOID;
        });

        // state-update! - Update state cell with function
        env.registerFunction("state-update!", params -> {
            if (params.length < 2) {
                throw new IllegalArgumentException("state-update! requires state cell and update function");
            }
            StateCell cell = params[0].asObject(StateCell.class);
            if (cell == null) {
                throw new IllegalArgumentException("First argument must be a state cell");
            }
            var updateFn = params[1].asFunction();
            if (updateFn == null) {
                throw new IllegalArgumentException("Second argument must be a function");
            }
            LispObject currentValue = cell.getValue();
            cell.setValue(updateFn.function().evaluate(new LispObject[]{currentValue}));
            return VoidObject.VOID;
        });
    }

    private void registerDesktopExtensions() {
        // toast - Print message to console
        env.registerFunction("toast", params -> {
            if (params.length > 0) {
                String message = params[0] instanceof StringObject
                    ? ((StringObject) params[0]).value()
                    : params[0].toString();
                System.out.println("[Toast] " + message);
            }
            return VoidObject.VOID;
        });

        // vibrate - No-op on desktop
        env.registerFunction("vibrate", params -> VoidObject.VOID);

        // dark-mode? - Always false on desktop
        env.registerFunction("dark-mode?", params -> BooleanObject.FALSE);

        // screen-width/height - Return default values
        env.registerFunction("screen-width", params -> new IntObject(1200));
        env.registerFunction("screen-height", params -> new IntObject(800));

        // android-version - Return 0 on desktop
        env.registerFunction("android-version", params -> new IntObject(0));

        // device-model - Return "Desktop"
        env.registerFunction("device-model", params -> new StringObject("Desktop (REPL)"));
    }

    private void registerUtilityFunctions() {
        // platform - Get current platform
        env.registerFunction("platform", params -> new JavaObject(Platform.DESKTOP_JVM));

        // platform? - Check if running on specific platform
        env.registerFunction("platform?", params -> {
            String platformName;
            if (params[0].asAtom() != null) {
                platformName = params[0].asAtom().toString();
            } else if (params[0].asString() != null) {
                platformName = params[0].asString().value();
            } else {
                throw new IllegalArgumentException("platform? expects a symbol or string");
            }

            boolean matches = platformName.equalsIgnoreCase("desktop") ||
                              platformName.equalsIgnoreCase("desktop-jvm") ||
                              platformName.equalsIgnoreCase("jvm");
            return matches ? params[0] : BooleanObject.FALSE;
        });

        // app-running? - Check if app window is open
        env.registerFunction("app-running?", params ->
            appRunning ? BooleanObject.TRUE : BooleanObject.FALSE);

        // close-app - Close the running app window
        env.registerFunction("close-app", params -> {
            if (appRunning && uiThread != null) {
                uiThread.interrupt();
                appRunning = false;
            }
            return VoidObject.VOID;
        });
    }

    private void registerRunAppFunction() {
        // run-app - Run a GUI application
        // Usage: (run-app app) where app is a function that returns a UIElement
        env.registerFunction("run-app", params -> {
            if (params.length == 0) {
                throw new IllegalArgumentException(
                    "run-app requires an app function argument.\n" +
                    "Usage: (run-app app) where app is defined as (define (app) (box ...))");
            }

            var appFn = params[0].asFunction();
            if (appFn == null) {
                throw new IllegalArgumentException(
                    "run-app expects a function, got: " + params[0].getClass().getSimpleName());
            }

            if (appRunning) {
                System.out.println("[run-app] App is already running. Use (close-app) first.");
                return BooleanObject.FALSE;
            }

            // Store reference to the app function for re-evaluation (as field for update-app)
            entryPointFunction = appFn.function();

            // Set up recomposition callback - uses the field so update-app can change it
            stateManager.setRecompositionCallback((Function0<Unit>) () -> {
                reEvaluateApp();
                return Unit.INSTANCE;
            });

            // Initial evaluation
            LispObject result = entryPointFunction.evaluate(new LispObject[0]);
            UIElement initialElement = buildUITree(result);

            // Read window configuration from environment
            int windowWidth = readIntVariable("*window-width*", 800);
            int windowHeight = readIntVariable("*window-height*", 900);

            // Launch UI on separate thread
            CountDownLatch startLatch = new CountDownLatch(1);
            AtomicReference<MutableState<UIElement>> rootRef = new AtomicReference<>();

            uiThread = new Thread(() -> {
                try {
                    appRunning = true;
                    ReplAppWindowKt.runReplAppWindow(
                        initialElement,
                        componentRegistry,
                        stateManager,
                        (Function1<MutableState<UIElement>, Unit>) root -> {
                            rootRef.set(root);
                            rootElement = root;
                            startLatch.countDown();
                            return Unit.INSTANCE;
                        },
                        (Function0<Unit>) () -> {
                            appRunning = false;
                            rootElement = null;
                            entryPointFunction = null;
                            System.out.println("[run-app] App window closed.");
                            return Unit.INSTANCE;
                        },
                        windowWidth,
                        windowHeight
                    );
                } catch (Exception e) {
                    System.err.println("[run-app] Error running app: " + e.getMessage());
                    e.printStackTrace();
                    appRunning = false;
                }
            }, "Moonstone-UI");

            uiThread.start();

            // Wait for window to start
            try {
                startLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            System.out.println("[run-app] App started. State changes will update the UI.");
            System.out.println("[run-app] Use (close-app) to close the window, or (update-app new-fn) to change the app.");
            return BooleanObject.TRUE;
        });

        // update-app - Update the running app's entry point function
        // Usage: (update-app new-app-fn)
        env.registerFunction("update-app", params -> {
            if (params.length == 0) {
                throw new IllegalArgumentException(
                    "update-app requires an app function argument.\n" +
                    "Usage: (update-app new-app-function)");
            }

            var appFn = params[0].asFunction();
            if (appFn == null) {
                throw new IllegalArgumentException(
                    "update-app expects a function, got: " + params[0].getClass().getSimpleName());
            }

            if (!appRunning) {
                System.out.println("[update-app] No app is running. Use (run-app app) first.");
                return BooleanObject.FALSE;
            }

            // Update the entry point function
            entryPointFunction = appFn.function();

            // Re-evaluate with new function
            reEvaluateApp();

            System.out.println("[update-app] App updated successfully.");
            return BooleanObject.TRUE;
        });
    }

    /**
     * Re-evaluate the app function and update the root element.
     */
    private void reEvaluateApp() {
        if (entryPointFunction == null) return;
        try {
            LispObject result = entryPointFunction.evaluate(new LispObject[0]);
            UIElement element = buildUITree(result);
            if (rootElement != null) {
                rootElement.setValue(element);
            }
        } catch (Exception e) {
            System.err.println("[repl] Error re-evaluating app: " + e.getMessage());
        }
    }

    private UIElement buildUITree(LispObject result) {
        UIElementWrapper wrapper = result.asObject(UIElementWrapper.class);
        if (wrapper != null) {
            return wrapper.getElement();
        }
        throw new IllegalStateException(
            "App function must return a UI element, got: " + result);
    }

    /**
     * Read an optional integer variable from the Lisp environment.
     * Returns the default value if the variable is not defined or is not a number.
     */
    private int readIntVariable(String name, int defaultValue) {
        try {
            var atom = env.atomOf(name);
            var value = env.lookupValueOrNull(atom);
            if (value == null) return defaultValue;

            // Try to get as integer
            var intObj = value.asInt();
            if (intObj != null) {
                return intObj.value();
            }

            // Try to get as double and convert
            var doubleObj = value.asDouble();
            if (doubleObj != null) {
                return (int) doubleObj.value();
            }

            return defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
