import { StreamLanguage, StringStream } from '@codemirror/language';

// Known Scheme/KleinLisp keywords
const keywords = new Set([
  'define', 'lambda', 'let', 'let*', 'letrec', 'if', 'cond', 'else', 'begin',
  'quote', 'quasiquote', 'unquote', 'unquote-splicing', 'set!', 'and', 'or', 'not',
  'when', 'unless', 'case', 'do',
]);

// Known KleinLisp builtins/components
const builtins = new Set([
  // State management
  'state', 'state-ref', 'state-set!', 'state-update!', 'derived',
  // Components
  'box', 'column', 'row', 'surface', 'spacer', 'text', 'button', 'text-field',
  'outlined-text-field', 'checkbox', 'switch', 'radio-button', 'icon',
  'lazy-column', 'lazy-row', 'list-item', 'dynamic-list',
  'scaffold', 'top-app-bar', 'bottom-navigation', 'nav-item',
  'alert-dialog', 'bottom-sheet', 'snackbar', 'switch-view', 'view',
  // List operations
  'car', 'cdr', 'cons', 'list', 'length', 'append', 'reverse', 'map', 'filter',
  'cadr', 'caddr', 'cadddr', 'fold-left', 'fold-right', 'find', 'any', 'all',
  'null?', 'pair?', 'list?', 'member', 'assoc', 'for-each',
  // Arithmetic
  '+', '-', '*', '/', '=', '<', '>', '<=', '>=', 'modulo', 'remainder',
  'abs', 'min', 'max', 'sqrt', 'expt', 'floor', 'ceiling', 'round',
  // Type conversions
  'number->string', 'string->number', 'symbol->string', 'string->symbol',
  // String functions
  'string-append', 'string-length', 'string=?', 'string<?', 'string>?',
  'string-contains?', 'string-trim', 'substring', 'string-ref',
  // Predicates
  'eq?', 'eqv?', 'equal?', 'number?', 'string?', 'symbol?', 'boolean?',
  'procedure?', 'zero?', 'positive?', 'negative?', 'odd?', 'even?',
  // I/O
  'display', 'newline', 'read', 'write',
  // Android
  'toast', 'vibrate', 'dark-mode?', 'screen-width', 'screen-height',
  'android-version', 'device-model', 'platform', 'platform?',
  'app-folder', 'read-app-file', 'write-app-file', 'app-file-exists?',
]);

// Scheme tokenizer for StreamLanguage
const schemeTokenizer = {
  token(stream: StringStream): string | null {
    // Skip whitespace
    if (stream.eatSpace()) return null;

    // Comments
    if (stream.match(';')) {
      stream.skipToEnd();
      return 'lineComment';
    }

    // Strings
    if (stream.match('"')) {
      let escaped = false;
      while (!stream.eol()) {
        const ch = stream.next();
        if (ch === '"' && !escaped) break;
        escaped = !escaped && ch === '\\';
      }
      return 'string';
    }

    // Property keywords (#:keyword)
    if (stream.match(/#:[a-zA-Z_\-]+/)) {
      return 'propertyName';
    }

    // Booleans
    if (stream.match('#t') || stream.match('#f')) {
      return 'atom';
    }

    // Numbers (including negative)
    if (stream.match(/-?[0-9]+(\.[0-9]+)?/)) {
      return 'number';
    }

    // Parentheses
    if (stream.match('(') || stream.match(')')) {
      return 'paren';
    }

    // Square brackets
    if (stream.match('[') || stream.match(']')) {
      return 'squareBracket';
    }

    // Quote
    if (stream.match("'")) {
      return 'meta';
    }

    // Symbols/identifiers
    if (stream.match(/[a-zA-Z_+\-*\/<>=!?][a-zA-Z0-9_+\-*\/<>=!?]*/)) {
      const word = stream.current();
      if (keywords.has(word)) {
        return 'keyword';
      }
      if (builtins.has(word)) {
        return 'macroName';  // Use macroName for built-in functions
      }
      return 'variableName';
    }

    // Single character operators
    if (stream.match(/[+\-*\/<>=]/)) {
      const op = stream.current();
      if (builtins.has(op)) {
        return 'macroName';
      }
      return 'operator';
    }

    // Skip unknown characters
    stream.next();
    return null;
  },
};

// Create the Scheme language
export const schemeLanguage = StreamLanguage.define(schemeTokenizer);

// Export keyword sets for autocomplete
export { keywords, builtins };
