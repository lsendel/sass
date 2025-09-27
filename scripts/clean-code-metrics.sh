#!/bin/bash

# Clean Code Metrics Analysis Script
# Analyzes codebase for clean code violations and generates detailed metrics

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

PROJECT_ROOT=$(pwd)
BACKEND_DIR="$PROJECT_ROOT/backend"
FRONTEND_DIR="$PROJECT_ROOT/frontend"
METRICS_DIR="$PROJECT_ROOT/metrics"

mkdir -p "$METRICS_DIR"

print_header() {
    echo -e "${BLUE}=== $1 ===${NC}"
}

print_metric() {
    local metric="$1"
    local value="$2"
    local threshold="$3"
    local status="$4"

    if [ "$status" = "GOOD" ]; then
        echo -e "${GREEN}✓${NC} $metric: $value (threshold: $threshold)"
    elif [ "$status" = "WARNING" ]; then
        echo -e "${YELLOW}⚠${NC} $metric: $value (threshold: $threshold)"
    else
        echo -e "${RED}✗${NC} $metric: $value (threshold: $threshold)"
    fi
}

# Analyze method/function length
analyze_method_length() {
    local dir="$1"
    local extension="$2"
    local language="$3"

    print_header "Method Length Analysis - $language"

    local temp_file="$METRICS_DIR/methods_$language.tmp"
    local long_methods=0
    local total_methods=0

    if [ "$language" = "Java" ]; then
        # Find Java methods
        find "$dir" -name "*.$extension" -exec awk '
        /^\s*(public|private|protected).*\(.*\)\s*\{/ {
            method_start = NR
            method_name = $0
            brace_count = 1
            next
        }
        brace_count > 0 {
            if ($0 ~ /\{/) brace_count++
            if ($0 ~ /\}/) brace_count--
            if (brace_count == 0) {
                length = NR - method_start + 1
                if (length > 20) {
                    print FILENAME ":" method_start ":" length ":" method_name
                }
                total++
                if (length > 20) long++
            }
        }
        END {
            print "STATS:" total ":" long > "/dev/stderr"
        }' {} + 2>"$temp_file.stats" >"$temp_file"

    elif [ "$language" = "TypeScript" ]; then
        # Find TypeScript functions/methods
        find "$dir" -name "*.$extension" -exec awk '
        /^\s*(function|const.*=.*\(|.*:\s*\(|export.*function)/ {
            if ($0 !~ /\/\//) {
                method_start = NR
                method_name = $0
                brace_count = 0
                paren_count = 0
                in_method = 1
            }
        }
        in_method {
            if ($0 ~ /\{/) brace_count++
            if ($0 ~ /\}/) brace_count--
            if (brace_count > 0 || $0 ~ /\{/) {
                if (brace_count == 0 && $0 ~ /\}/) {
                    length = NR - method_start + 1
                    if (length > 20) {
                        print FILENAME ":" method_start ":" length ":" method_name
                    }
                    total++
                    if (length > 20) long++
                    in_method = 0
                }
            }
        }
        END {
            print "STATS:" total ":" long > "/dev/stderr"
        }' {} + 2>"$temp_file.stats" >"$temp_file"
    fi

    if [ -f "$temp_file.stats" ]; then
        read -r stats_line < "$temp_file.stats"
        total_methods=$(echo "$stats_line" | cut -d: -f2)
        long_methods=$(echo "$stats_line" | cut -d: -f3)
    fi

    # Calculate percentage
    if [ "$total_methods" -gt 0 ]; then
        percentage=$((long_methods * 100 / total_methods))
        if [ "$percentage" -le 10 ]; then
            status="GOOD"
        elif [ "$percentage" -le 20 ]; then
            status="WARNING"
        else
            status="BAD"
        fi
        print_metric "Long methods (>20 lines)" "$long_methods/$total_methods ($percentage%)" "≤10%" "$status"
    else
        print_metric "Long methods" "No methods found" "N/A" "WARNING"
    fi

    # Show worst offenders
    if [ -f "$temp_file" ] && [ -s "$temp_file" ]; then
        echo "Longest methods:"
        head -10 "$temp_file" | while IFS=: read -r file line length method; do
            echo "  $file:$line - $length lines"
        done
    fi

    rm -f "$temp_file" "$temp_file.stats"
}

# Analyze class/component size
analyze_class_size() {
    local dir="$1"
    local extension="$2"
    local language="$3"

    print_header "Class/Component Size Analysis - $language"

    local large_files=0
    local total_files=0
    local temp_file="$METRICS_DIR/large_files_$language.txt"

    find "$dir" -name "*.$extension" | while read -r file; do
        lines=$(wc -l < "$file")
        total_files=$((total_files + 1))
        if [ "$lines" -gt 200 ]; then
            echo "$file:$lines" >> "$temp_file"
            large_files=$((large_files + 1))
        fi
    done

    total_files=$(find "$dir" -name "*.$extension" | wc -l)
    large_files=0
    if [ -f "$temp_file" ]; then
        large_files=$(wc -l < "$temp_file")
    fi

    if [ "$total_files" -gt 0 ]; then
        percentage=$((large_files * 100 / total_files))
        if [ "$percentage" -le 5 ]; then
            status="GOOD"
        elif [ "$percentage" -le 15 ]; then
            status="WARNING"
        else
            status="BAD"
        fi
        print_metric "Large files (>200 lines)" "$large_files/$total_files ($percentage%)" "≤5%" "$status"
    fi

    if [ -f "$temp_file" ] && [ -s "$temp_file" ]; then
        echo "Largest files:"
        sort -t: -k2 -nr "$temp_file" | head -10 | while IFS=: read -r file lines; do
            echo "  $file - $lines lines"
        done
    fi

    rm -f "$temp_file"
}

# Analyze cyclomatic complexity (simplified)
analyze_complexity() {
    local dir="$1"
    local extension="$2"
    local language="$3"

    print_header "Complexity Analysis - $language"

    local complex_methods=0
    local total_methods=0

    # Count decision points (if, while, for, case, catch, &&, ||)
    find "$dir" -name "*.$extension" -exec grep -n -E "(if\s*\(|while\s*\(|for\s*\(|case\s|catch\s*\(|\&\&|\|\|)" {} + > "$METRICS_DIR/complexity_$language.tmp"

    if [ -f "$METRICS_DIR/complexity_$language.tmp" ]; then
        local decision_points=$(wc -l < "$METRICS_DIR/complexity_$language.tmp")
        local file_count=$(find "$dir" -name "*.$extension" | wc -l)

        if [ "$file_count" -gt 0 ]; then
            local avg_complexity=$((decision_points / file_count))
            if [ "$avg_complexity" -le 5 ]; then
                status="GOOD"
            elif [ "$avg_complexity" -le 10 ]; then
                status="WARNING"
            else
                status="BAD"
            fi
            print_metric "Average complexity per file" "$avg_complexity decision points" "≤5" "$status"
        fi

        echo "Files with highest complexity:"
        awk -F: '{print $1}' "$METRICS_DIR/complexity_$language.tmp" | sort | uniq -c | sort -nr | head -5 | while read -r count file; do
            echo "  $file - $count decision points"
        done
    fi

    rm -f "$METRICS_DIR/complexity_$language.tmp"
}

# Analyze code duplication (simplified)
analyze_duplication() {
    local dir="$1"
    local extension="$2"
    local language="$3"

    print_header "Code Duplication Analysis - $language"

    # Look for repeated function/method signatures
    find "$dir" -name "*.$extension" -exec grep -h -E "(function|public|private|const.*=)" {} + | \
        sort | uniq -c | sort -nr | head -10 > "$METRICS_DIR/duplicates_$language.txt"

    local duplicates=$(awk '$1 > 1 {count++} END {print count+0}' "$METRICS_DIR/duplicates_$language.txt")

    if [ "$duplicates" -le 2 ]; then
        status="GOOD"
    elif [ "$duplicates" -le 5 ]; then
        status="WARNING"
    else
        status="BAD"
    fi

    print_metric "Potential duplicated signatures" "$duplicates" "≤2" "$status"

    if [ "$duplicates" -gt 0 ]; then
        echo "Most repeated patterns:"
        head -5 "$METRICS_DIR/duplicates_$language.txt" | while read -r count pattern; do
            if [ "$count" -gt 1 ]; then
                echo "  $count times: $(echo "$pattern" | sed 's/^[[:space:]]*//' | cut -c1-60)..."
            fi
        done
    fi
}

# Analyze naming conventions
analyze_naming() {
    local dir="$1"
    local extension="$2"
    local language="$3"

    print_header "Naming Convention Analysis - $language"

    local violations=0

    if [ "$language" = "Java" ]; then
        # Check for camelCase violations in Java
        violations=$(find "$dir" -name "*.$extension" -exec grep -n -E "(class|interface|enum)\s+[a-z]" {} + | wc -l)
        violations=$((violations + $(find "$dir" -name "*.$extension" -exec grep -n -E "(public|private|protected).*[A-Z_]+\s*\(" {} + | wc -l)))
    elif [ "$language" = "TypeScript" ]; then
        # Check for camelCase violations in TypeScript
        violations=$(find "$dir" -name "*.$extension" -exec grep -n -E "(function|const|let)\s+[A-Z]" {} + | wc -l)
    fi

    if [ "$violations" -le 5 ]; then
        status="GOOD"
    elif [ "$violations" -le 15 ]; then
        status="WARNING"
    else
        status="BAD"
    fi

    print_metric "Naming convention violations" "$violations" "≤5" "$status"
}

# Analyze magic numbers
analyze_magic_numbers() {
    local dir="$1"
    local extension="$2"
    local language="$3"

    print_header "Magic Numbers Analysis - $language"

    # Find numeric literals (excluding 0, 1, -1)
    local magic_numbers=$(find "$dir" -name "*.$extension" -exec grep -n -E "[^a-zA-Z0-9_](([2-9]|[1-9][0-9]+)(\.[0-9]+)?)[^a-zA-Z0-9_]" {} + | wc -l)

    if [ "$magic_numbers" -le 10 ]; then
        status="GOOD"
    elif [ "$magic_numbers" -le 25 ]; then
        status="WARNING"
    else
        status="BAD"
    fi

    print_metric "Magic numbers found" "$magic_numbers" "≤10" "$status"

    # Show examples
    find "$dir" -name "*.$extension" -exec grep -n -E "[^a-zA-Z0-9_](([2-9]|[1-9][0-9]+)(\.[0-9]+)?)[^a-zA-Z0-9_]" {} + | head -5 | while IFS=: read -r file line content; do
        echo "  $file:$line"
    done
}

# Generate metrics report
generate_metrics_report() {
    print_header "Generating Metrics Report"

    cat > "$METRICS_DIR/clean-code-metrics.md" << 'EOF'
# Clean Code Metrics Report

Generated on: $(date)

## Methodology

This report analyzes the codebase for common clean code violations:

- **Method Length**: Functions/methods longer than 20 lines
- **Class Size**: Files longer than 200 lines
- **Complexity**: High number of decision points (if, while, for, etc.)
- **Duplication**: Repeated code patterns
- **Naming**: Convention violations
- **Magic Numbers**: Hardcoded numeric values

## Scoring

- ✅ **GOOD**: Meets clean code standards
- ⚠️ **WARNING**: Needs attention
- ❌ **BAD**: Requires immediate refactoring

## Backend Analysis (Java)

EOF

    echo "Backend metrics analysis completed"
    echo ""
    echo "## Frontend Analysis (TypeScript)" >> "$METRICS_DIR/clean-code-metrics.md"
    echo "Frontend metrics analysis completed" >> "$METRICS_DIR/clean-code-metrics.md"

    echo "Metrics report generated: $METRICS_DIR/clean-code-metrics.md"
}

# Main execution
main() {
    print_header "Clean Code Metrics Analysis"

    # Backend analysis
    if [ -d "$BACKEND_DIR" ]; then
        echo -e "${BLUE}Analyzing Backend (Java)${NC}"
        analyze_method_length "$BACKEND_DIR/src" "java" "Java"
        echo ""
        analyze_class_size "$BACKEND_DIR/src" "java" "Java"
        echo ""
        analyze_complexity "$BACKEND_DIR/src" "java" "Java"
        echo ""
        analyze_duplication "$BACKEND_DIR/src" "java" "Java"
        echo ""
        analyze_naming "$BACKEND_DIR/src" "java" "Java"
        echo ""
        analyze_magic_numbers "$BACKEND_DIR/src" "java" "Java"
        echo ""
    fi

    # Frontend analysis
    if [ -d "$FRONTEND_DIR" ]; then
        echo -e "${BLUE}Analyzing Frontend (TypeScript)${NC}"
        analyze_method_length "$FRONTEND_DIR/src" "ts" "TypeScript"
        echo ""
        analyze_method_length "$FRONTEND_DIR/src" "tsx" "TypeScript"
        echo ""
        analyze_class_size "$FRONTEND_DIR/src" "ts" "TypeScript"
        echo ""
        analyze_class_size "$FRONTEND_DIR/src" "tsx" "TypeScript"
        echo ""
        analyze_complexity "$FRONTEND_DIR/src" "ts" "TypeScript"
        echo ""
        analyze_complexity "$FRONTEND_DIR/src" "tsx" "TypeScript"
        echo ""
        analyze_duplication "$FRONTEND_DIR/src" "ts" "TypeScript"
        echo ""
        analyze_duplication "$FRONTEND_DIR/src" "tsx" "TypeScript"
        echo ""
        analyze_naming "$FRONTEND_DIR/src" "ts" "TypeScript"
        echo ""
        analyze_naming "$FRONTEND_DIR/src" "tsx" "TypeScript"
        echo ""
        analyze_magic_numbers "$FRONTEND_DIR/src" "ts" "TypeScript"
        echo ""
        analyze_magic_numbers "$FRONTEND_DIR/src" "tsx" "TypeScript"
        echo ""
    fi

    generate_metrics_report

    print_header "Analysis Complete"
    echo "Detailed metrics saved in: $METRICS_DIR/"
}

main "$@"