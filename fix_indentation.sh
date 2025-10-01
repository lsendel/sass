
#!/bin/bash

# Script to fix common checkstyle indentation issues in Java files

find backend/src -name "*.java" -type f | while read -r file; do
    echo "Fixing indentation in: $file"

    # Fix method body indentation (lines starting with 12 spaces should be 16)
    sed -i '' 's/^            \(.*\)/                \1/' "$file"

    # Fix field/method indentation (lines starting with 4 spaces should be 8)
    sed -i '' 's/^    \([a-zA-Z@]\)/        \1/' "$file"

    # Fix constructor/field indentation (lines starting with 8 spaces might need adjustment)
    # This is more complex, applying basic fixes

done

echo "Indentation fixing completed."
