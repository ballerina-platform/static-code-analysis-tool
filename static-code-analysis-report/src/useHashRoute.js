import { useCallback, useEffect, useState } from "react";

// The report is opened straight from disk (file://), so routing lives in the URL hash:
//   #/                          -> project overview
//   #/file/<fileKey>            -> single file view
//   #/file/<fileKey>/issue/<n>  -> single file view with issue <n> expanded
function parseHash(hash) {
    const match = /^#\/file\/([^/]+)(?:\/issue\/(\d+))?\/?$/.exec(hash);
    if (!match) {
        return { view: "main", fileKey: null, issueIndex: null };
    }
    let fileKey = null;
    try {
        fileKey = decodeURIComponent(match[1]);
    } catch (e) {
        return { view: "main", fileKey: null, issueIndex: null };
    }
    return {
        view: "file",
        fileKey,
        issueIndex: match[2] !== undefined ? Number(match[2]) : null,
    };
}

export const buildFileHash = (fileKey, issueIndex = null) =>
    `#/file/${encodeURIComponent(fileKey)}${issueIndex !== null ? `/issue/${issueIndex}` : ""}`;

function useHashRoute() {
    const [route, setRoute] = useState(() => parseHash(window.location.hash));

    useEffect(() => {
        const onChange = () => setRoute(parseHash(window.location.hash));
        window.addEventListener("hashchange", onChange);
        window.addEventListener("popstate", onChange);
        return () => {
            window.removeEventListener("hashchange", onChange);
            window.removeEventListener("popstate", onChange);
        };
    }, []);

    // Moving between pages adds a history entry so the browser back button works.
    const openFile = useCallback((fileKey, issueIndex = null) => {
        window.location.hash = buildFileHash(fileKey, issueIndex);
        window.scrollTo({ top: 0 });
    }, []);

    const openMain = useCallback(() => {
        window.location.hash = "#/";
    }, []);

    // Expanding/collapsing an issue only replaces the entry, so it doesn't flood the back history.
    const selectIssue = useCallback((fileKey, issueIndex) => {
        const hash = buildFileHash(fileKey, issueIndex);
        window.history.replaceState(null, "", hash);
        setRoute(parseHash(hash));
    }, []);

    return { route, openFile, openMain, selectIssue };
}

export default useHashRoute;
