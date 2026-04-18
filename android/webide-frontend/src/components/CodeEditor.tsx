import { useEffect, useRef } from 'react';
import { EditorState } from '@codemirror/state';
import { EditorView, keymap, highlightActiveLine, lineNumbers, highlightActiveLineGutter } from '@codemirror/view';
import { defaultKeymap, history, historyKeymap } from '@codemirror/commands';
import { syntaxHighlighting, HighlightStyle, bracketMatching } from '@codemirror/language';
import { tags as t } from '@lezer/highlight';
import { searchKeymap, highlightSelectionMatches } from '@codemirror/search';
import { autocompletion, completionKeymap, CompletionContext } from '@codemirror/autocomplete';
import { schemeLanguage, keywords, builtins } from '../codemirror/scheme-lang';

interface CodeEditorProps {
  content: string;
  onChange: (content: string) => void;
  onSave: () => void;
}

// Dark theme for the editor
const darkTheme = EditorView.theme({
  '&': {
    backgroundColor: '#1e1e1e',
    color: '#d4d4d4',
  },
  '.cm-content': {
    caretColor: '#fff',
    fontFamily: "'Fira Code', 'Consolas', 'Monaco', monospace",
    fontSize: '14px',
    lineHeight: '1.6',
  },
  '.cm-cursor': {
    borderLeftColor: '#fff',
  },
  '.cm-selectionBackground, &.cm-focused .cm-selectionBackground': {
    backgroundColor: '#264f78',
  },
  '.cm-activeLine': {
    backgroundColor: '#2a2a2a',
  },
  '.cm-activeLineGutter': {
    backgroundColor: '#2a2a2a',
  },
  '.cm-gutters': {
    backgroundColor: '#1e1e1e',
    color: '#858585',
    borderRight: '1px solid #3c3c3c',
  },
  '.cm-lineNumbers .cm-gutterElement': {
    padding: '0 8px 0 16px',
  },
  '.cm-matchingBracket': {
    backgroundColor: '#3c3c3c',
    outline: '1px solid #888',
  },
});

// Syntax highlighting colors (VS Code dark theme inspired)
const syntaxColors = HighlightStyle.define([
  { tag: t.keyword, color: '#c586c0' },           // Purple for keywords (define, lambda, if)
  { tag: t.variableName, color: '#9cdcfe' },      // Light blue for variables
  { tag: t.macroName, color: '#dcdcaa' },         // Yellow for built-in functions
  { tag: t.function(t.variableName), color: '#dcdcaa' },  // Yellow for functions
  { tag: t.definition(t.variableName), color: '#4ec9b0' }, // Teal for definitions
  { tag: t.string, color: '#ce9178' },            // Orange for strings
  { tag: t.number, color: '#b5cea8' },            // Light green for numbers
  { tag: t.bool, color: '#569cd6' },              // Blue for booleans
  { tag: t.atom, color: '#569cd6' },              // Blue for #t, #f
  { tag: t.comment, color: '#6a9955' },           // Green for comments
  { tag: t.lineComment, color: '#6a9955' },       // Green for line comments
  { tag: t.paren, color: '#ffd700' },             // Gold for parentheses
  { tag: t.squareBracket, color: '#ffd700' },     // Gold for square brackets
  { tag: t.bracket, color: '#ffd700' },
  { tag: t.propertyName, color: '#9cdcfe' },      // Light blue for #:keywords
  { tag: t.operator, color: '#d4d4d4' },          // Default for operators
  { tag: t.meta, color: '#569cd6' },              // Blue for quote (')
]);

// Autocomplete for Scheme
function schemeCompletion(context: CompletionContext) {
  const word = context.matchBefore(/[a-zA-Z_\-!?*+/<>=][a-zA-Z0-9_\-!?*+/<>=]*/);
  if (!word) return null;

  const options = [
    ...Array.from(keywords).map(k => ({ label: k, type: 'keyword' })),
    ...Array.from(builtins).map(b => ({ label: b, type: 'function' })),
    // Common property keywords
    { label: '#:padding', type: 'property' },
    { label: '#:padding-horizontal', type: 'property' },
    { label: '#:padding-vertical', type: 'property' },
    { label: '#:spacing', type: 'property' },
    { label: '#:value', type: 'property' },
    { label: '#:on-click', type: 'property' },
    { label: '#:on-change', type: 'property' },
    { label: '#:on-select', type: 'property' },
    { label: '#:on-dismiss', type: 'property' },
    { label: '#:style', type: 'property' },
    { label: '#:color', type: 'property' },
    { label: '#:enabled', type: 'property' },
    { label: '#:checked', type: 'property' },
    { label: '#:selected', type: 'property' },
    { label: '#:visible', type: 'property' },
    { label: '#:label', type: 'property' },
    { label: '#:title', type: 'property' },
    { label: '#:text', type: 'property' },
    { label: '#:icon', type: 'property' },
    { label: '#:name', type: 'property' },
    { label: '#:key', type: 'property' },
    { label: '#:items', type: 'property' },
    { label: '#:render-item', type: 'property' },
    { label: '#:fill-max-width', type: 'property' },
    { label: '#:fill-max-height', type: 'property' },
    { label: '#:fill-max-size', type: 'property' },
    { label: '#:width', type: 'property' },
    { label: '#:height', type: 'property' },
    { label: '#:size', type: 'property' },
    { label: '#:background', type: 'property' },
    { label: '#:tint', type: 'property' },
    { label: '#:elevation', type: 'property' },
    { label: '#:shape', type: 'property' },
    { label: '#:vertical-arrangement', type: 'property' },
    { label: '#:horizontal-alignment', type: 'property' },
    { label: '#:horizontal-arrangement', type: 'property' },
    { label: '#:vertical-alignment', type: 'property' },
    { label: '#:content-alignment', type: 'property' },
    { label: '#:font-size', type: 'property' },
    { label: '#:font-weight', type: 'property' },
    { label: '#:max-lines', type: 'property' },
    { label: '#:single-line', type: 'property' },
    { label: '#:placeholder', type: 'property' },
    { label: '#:keyboard-type', type: 'property' },
  ];

  return {
    from: word.from,
    options: options.filter(o =>
      o.label.toLowerCase().startsWith(word.text.toLowerCase())
    ),
  };
}

export function CodeEditor({ content, onChange, onSave }: CodeEditorProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const viewRef = useRef<EditorView | null>(null);
  const onChangeRef = useRef(onChange);
  const onSaveRef = useRef(onSave);

  // Keep refs updated
  onChangeRef.current = onChange;
  onSaveRef.current = onSave;

  // Initialize editor
  useEffect(() => {
    if (!containerRef.current) return;

    const saveKeymap = keymap.of([
      {
        key: 'Mod-s',
        run: () => {
          onSaveRef.current();
          return true;
        },
      },
    ]);

    const state = EditorState.create({
      doc: content,
      extensions: [
        lineNumbers(),
        highlightActiveLineGutter(),
        highlightActiveLine(),
        history(),
        bracketMatching(),
        highlightSelectionMatches(),
        schemeLanguage,
        autocompletion({
          override: [schemeCompletion],
        }),
        darkTheme,
        syntaxHighlighting(syntaxColors),
        keymap.of([
          ...defaultKeymap,
          ...historyKeymap,
          ...searchKeymap,
          ...completionKeymap,
        ]),
        saveKeymap,
        EditorView.updateListener.of((update) => {
          if (update.docChanged) {
            onChangeRef.current(update.state.doc.toString());
          }
        }),
      ],
    });

    const view = new EditorView({
      state,
      parent: containerRef.current,
    });

    viewRef.current = view;

    return () => {
      view.destroy();
      viewRef.current = null;
    };
  }, []);

  // Update content when it changes externally
  useEffect(() => {
    const view = viewRef.current;
    if (!view) return;

    const currentContent = view.state.doc.toString();
    if (content !== currentContent) {
      view.dispatch({
        changes: {
          from: 0,
          to: view.state.doc.length,
          insert: content,
        },
      });
    }
  }, [content]);

  return <div ref={containerRef} style={{ height: '100%' }} />;
}
