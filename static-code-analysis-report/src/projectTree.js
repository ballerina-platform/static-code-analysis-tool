import { getFileKey } from "./issueMeta";

export const PROJECT_KINDS = {
    WORKSPACE: "WORKSPACE_PROJECT",
    PACKAGE: "BUILD_PROJECT",
};

// Path from the project (or workspace) root; older reports and files outside the root only have a name.
export const getRelativePath = (file) => file?.relativePath ?? file?.fileName ?? "";

export const getBaseName = (file) => getRelativePath(file).split("/").pop();

export const getFolderPath = (file) => getRelativePath(file).split("/").slice(0, -1).join("/");

export const findPackage = (packages, file) =>
    (packages ?? []).find(({ name }) => name === file?.packageName);

export const packageLabel = (pkg) => pkg.org ? `${pkg.org}/${pkg.name}` : pkg.name;

const emptyCounts = () => ({ codeSmells: 0, bugs: 0, vulnerabilities: 0, totalIssues: 0, fileCount: 0 });

const COUNT_FIELDS = ["codeSmells", "bugs", "vulnerabilities", "totalIssues", "fileCount"];

// Builds a folder tree from file records. In a workspace, the folder at each package's path becomes a
// package node so packages stand apart from plain folders.
export function buildTree(records, packages, workspace) {
    const packageByPath = new Map(workspace
        ? (packages ?? []).filter(({ path }) => path).map((pkg) => [pkg.path, pkg])
        : []);
    const root = { id: "", name: "", type: "folder", children: new Map() };

    records.forEach((record) => {
        const segments = getRelativePath(record.file).split("/");
        let node = root;
        let path = "";
        segments.slice(0, -1).forEach((segment) => {
            path = path ? `${path}/${segment}` : segment;
            if (!node.children.has(segment)) {
                const pkg = packageByPath.get(path);
                node.children.set(segment, {
                    id: path, name: segment, type: pkg ? "package" : "folder", pkg, children: new Map(),
                });
            }
            node = node.children.get(segment);
        });
        const name = segments[segments.length - 1];
        // File keys can't clash with folder segment names since they're full paths.
        node.children.set(`file:${getFileKey(record.file)}`, {
            id: `file:${getFileKey(record.file)}`, name, type: "file", record,
        });
    });

    return finalize(root, true);
}

function finalize(node, isRoot = false) {
    if (node.type === "file") {
        const { record } = node;
        return { ...node, counts: { ...pick(record), fileCount: 1 } };
    }
    let children = [...node.children.values()].map((child) => finalize(child));
    let { id, name } = node;
    // Compact single-folder chains (modules/util) like most code browsers, but never merge into a package.
    if (!isRoot && node.type === "folder") {
        while (children.length === 1 && children[0].type === "folder") {
            const [only] = children;
            id = only.id;
            name = `${name}/${only.name}`;
            children = only.children;
        }
    }
    const counts = emptyCounts();
    children.forEach((child) => COUNT_FIELDS.forEach((field) => { counts[field] += child.counts[field]; }));
    return { ...node, id, name, children, counts };
}

const pick = (record) => ({
    codeSmells: record.codeSmells,
    bugs: record.bugs,
    vulnerabilities: record.vulnerabilities,
    totalIssues: record.totalIssues,
});

export const collectFolderIds = (node) => node.type === "file"
    ? []
    : [...(node.id ? [node.id] : []), ...node.children.flatMap(collectFolderIds)];

// Folders always stay above files, as in most file explorers; the sort applies within each group.
const compareNodes = (sort) => (a, b) => {
    const direction = sort.direction === "asc" ? 1 : -1;
    const byName = a.name.localeCompare(b.name, undefined, { numeric: true });
    const folderFirst = (b.type !== "file") - (a.type !== "file");
    if (sort.field === "name") {
        return folderFirst || direction * byName;
    }
    return folderFirst || direction * (a.counts[sort.field] - b.counts[sort.field]) || byName;
};

// Flattens the visible part of the tree into table rows; collapsed folders hide their descendants.
export function flattenTree(root, collapsed, sort) {
    const rows = [];
    const visit = (node, depth) => {
        [...node.children].sort(compareNodes(sort)).forEach((child) => {
            rows.push({ node: child, depth });
            if (child.type !== "file" && !collapsed.has(child.id)) {
                visit(child, depth + 1);
            }
        });
    };
    visit(root, 0);
    return rows;
}

export function flattenList(records, sort) {
    const nodes = records.map((record) => ({
        id: `file:${getFileKey(record.file)}`,
        name: getRelativePath(record.file),
        type: "file",
        record,
        counts: { ...pick(record), fileCount: 1 },
    }));
    return nodes.sort(compareNodes(sort)).map((node) => ({ node, depth: 0 }));
}
