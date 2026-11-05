(ns heraldicon.frontend.entity.action.export-download)

(defn- click-link [href download]
  (let [link (js/document.createElement "a")]
    (set! (.-href link) href)
    (set! (.-rel link) "noopener")
    (.setAttribute link "download" download)
    (.appendChild js/document.body link)
    (.click link)
    (.removeChild js/document.body link)))

(defn trigger
  "Trigger a browser download of `url`. Exported files are served from S3 with a
   Content-Disposition: attachment header (carrying the proper filename), so a
   programmatic link click downloads the file in place rather than navigating."
  [url]
  (click-link url ""))

(defn trigger-blob
  "Trigger a browser download of a blob created in the browser as `filename`."
  [blob filename]
  (let [url (js/URL.createObjectURL blob)]
    (click-link url filename)
    ;; revoking the URL at once can cancel the download in some browsers
    (js/setTimeout #(js/URL.revokeObjectURL url) 1000)))
