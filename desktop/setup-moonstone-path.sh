#!/bin/bash
# Manual setup script for Moonstone PATH configuration
# Use this if the DEB/RPM post-install scripts don't run due to the Compose plugin bug

set -e

echo "Setting up Moonstone in PATH..."

# Find the installation directory
INSTALL_DIR="/opt/moonstone"

if [ ! -d "$INSTALL_DIR/bin" ]; then
    echo "Error: Moonstone installation not found at $INSTALL_DIR"
    echo "Please ensure Moonstone is installed first"
    exit 1
fi

# Check if moonstone executable exists
if [ ! -f "$INSTALL_DIR/bin/moonstone" ]; then
    echo "Error: Moonstone executable not found at $INSTALL_DIR/bin/moonstone"
    exit 1
fi

# Create symlink (requires sudo)
if [ ! -L /usr/bin/moonstone ]; then
    echo "Creating symlink in /usr/bin (requires sudo)..."
    sudo ln -sf "$INSTALL_DIR/bin/moonstone" /usr/bin/moonstone
    echo "✓ Symlink created successfully"
else
    echo "✓ Symlink already exists"
fi

# Verify installation
if command -v moonstone &> /dev/null; then
    echo "✓ Moonstone is now available in PATH"
    echo "  You can run: moonstone /path/to/app.scm"
else
    echo "Warning: moonstone command not found in PATH"
    echo "You may need to restart your terminal or log out and back in"
fi

echo "Done!"
