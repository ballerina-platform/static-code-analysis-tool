import { FilterAltOffOutlined, Search } from "@mui/icons-material";
import {
    Autocomplete,
    Box,
    Button,
    Checkbox,
    Chip,
    InputAdornment,
    TextField,
    Typography,
    alpha
} from "@mui/material";
import { useMemo } from "react";
import { EMPTY_FILTERS, RULE_KINDS, RULE_KIND_ORDER, collectFilterOptions, countActiveFilters } from "../issueMeta";

const FIELD_LABELS = {
    severities: "Severity",
    rules: "Rule",
    tags: "Tags",
    cwes: "CWE",
    owasp: "OWASP Top 10",
};

const inputSx = { "& .MuiOutlinedInput-root": { borderRadius: "0.5rem", bgcolor: "#ffffff" } };

// Toggle chips for rule kinds, colored with the shared rule-kind palette.
export const KindFilterChips = ({ counts, selected, onChange }) => (
    <Box sx={{ display: "flex", flexWrap: "wrap", alignItems: "center", gap: "0.5rem" }}>
        {RULE_KIND_ORDER.filter((kind) => counts[kind]).map((kind) => {
            const { Icon, color, plural } = RULE_KINDS[kind];
            const active = selected.includes(kind);
            return (
                <Chip
                    key={kind}
                    clickable
                    onClick={() => onChange(active ? selected.filter((k) => k !== kind) : [...selected, kind])}
                    icon={<Icon style={{ color: active ? "#ffffff" : color, fontSize: 16 }} />}
                    label={`${plural} · ${counts[kind]}`}
                    variant={active ? "filled" : "outlined"}
                    sx={{
                        fontWeight: 600,
                        borderColor: alpha(color, 0.6),
                        bgcolor: active ? color : "transparent",
                        color: active ? "#ffffff" : "text.primary",
                        "&:hover": { bgcolor: active ? color : alpha(color, 0.1) },
                        "&.MuiChip-clickable:hover": { bgcolor: active ? color : alpha(color, 0.1) },
                    }}
                />
            );
        })}
    </Box>
);

export const SearchField = ({ value, onChange, placeholder, width = 240 }) => (
    <TextField
        size="small"
        placeholder={placeholder}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        InputProps={{
            startAdornment: <InputAdornment position="start"><Search fontSize="small" /></InputAdornment>,
        }}
        sx={{ width: { xs: "100%", sm: width }, ...inputSx }}
    />
);

const MultiSelectFilter = ({ label, options, selected, onChange }) => {
    // Keep selections visible even if no issue in the current scope carries them any more.
    const byValue = new Map(options.map((option) => [option.value, option]));
    const value = selected.map((v) => byValue.get(v) ?? { value: v, label: v, count: 0 });
    return (
        <Autocomplete
            multiple
            size="small"
            disableCloseOnSelect
            limitTags={2}
            options={options}
            value={value}
            onChange={(_, next) => onChange(next.map((option) => option.value))}
            isOptionEqualToValue={(option, v) => option.value === v.value}
            getOptionLabel={(option) => option.label}
            noOptionsText={`No ${label.toLowerCase()} in these issues`}
            renderOption={(props, option, { selected: checked }) => {
                const { key, ...optionProps } = props;
                return (
                    <li key={key} {...optionProps} style={{ ...optionProps.style, gap: "4px", fontSize: "14px" }}>
                        <Checkbox size="small" checked={checked} sx={{ padding: "2px", marginRight: "4px" }} />
                        <Box component="span" sx={{ flex: 1, minWidth: 0 }}>{option.label}</Box>
                        <Typography variant="caption" color="text.secondary">{option.count}</Typography>
                    </li>
                );
            }}
            renderTags={(tagValue, getTagProps) => tagValue.map((option, index) => {
                const { key, ...tagProps } = getTagProps({ index });
                // Chips show the short code (e.g. "A05:2025"), the dropdown shows the full title.
                return <Chip key={key} size="small" label={option.label.split(" · ")[0]} {...tagProps} sx={{ height: 22 }} />;
            })}
            renderInput={(params) => <TextField {...params} label={label} placeholder={selected.length ? "" : "Any"} />}
            sx={{ minWidth: 180, flex: "1 1 180px", maxWidth: { md: 320 }, ...inputSx }}
        />
    );
};

// `fields` picks which dropdowns to show; the kind chips and the search box are rendered by callers
// that want them, since each page places them differently.
export const IssueFilterFields = ({ issues, filters, onChange, fields = Object.keys(FIELD_LABELS) }) => {
    const options = useMemo(() => collectFilterOptions(issues), [issues]);
    return (
        <Box sx={{ display: "flex", flexWrap: "wrap", alignItems: "center", gap: "0.75rem" }}>
            {fields.map((field) => (
                <MultiSelectFilter
                    key={field}
                    label={FIELD_LABELS[field]}
                    options={options[field]}
                    selected={filters[field]}
                    onChange={(next) => onChange({ ...filters, [field]: next })}
                />
            ))}
        </Box>
    );
};

export const ClearFiltersButton = ({ filters, onChange }) => {
    const active = countActiveFilters(filters);
    if (active === 0) {
        return null;
    }
    return (
        <Button
            size="small"
            startIcon={<FilterAltOffOutlined />}
            onClick={() => onChange(EMPTY_FILTERS)}
            sx={{ textTransform: "none", fontWeight: 600, borderRadius: "0.5rem" }}
        >
            Clear filters ({active})
        </Button>
    );
};
