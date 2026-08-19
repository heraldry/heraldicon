(ns heraldicon.frontend.entity.action.export-download)

(defn trigger
  "Trigger a browser download of `url`. Exported files are served from S3 with a
   Content-Disposition: attachment header (carrying the proper filename), so a
   programmatic link click downloads the file in place rather than navigating."
  [url]
  (let [link (js/document.createElement "a")]
    (set! (.-href link) url)
    (set! (.-rel link) "noopener")
    (.setAttribute link "download" "")
    (.appendChild js/document.body link)
    (.click link)
    (.removeChild js/document.body link)))
