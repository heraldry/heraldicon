(ns heraldicon.frontend.blazonry-editor.dom)

(def input-id
  "blazonry-editor-input")

(def ^:private mirrored-properties
  ["box-sizing" "width" "font-family" "font-size" "font-weight" "font-style"
   "letter-spacing" "word-spacing" "line-height" "text-transform" "tab-size"
   "padding-top" "padding-right" "padding-bottom" "padding-left"
   "border-top-width" "border-right-width" "border-bottom-width" "border-left-width"
   "white-space" "overflow-wrap" "word-break" "scrollbar-gutter" "overflow-y"])

(defn caret-position
  "Viewport position of the character at index in the editor's textarea,
  measured with a hidden copy that has the same styles and wraps the same way."
  [index]
  (when-let [^js textarea (js/document.getElementById input-id)]
    (let [text (.-value textarea)
          index (-> (or index (.-selectionStart textarea))
                    (max 0)
                    (min (count text)))
          styles (js/getComputedStyle textarea)
          rect (.getBoundingClientRect textarea)
          ^js mirror (js/document.createElement "div")
          ^js marker (js/document.createElement "span")]
      (doseq [property mirrored-properties]
        (.setProperty (.-style mirror) property (.getPropertyValue styles property)))
      (doto (.-style mirror)
        (.setProperty "position" "fixed")
        (.setProperty "visibility" "hidden")
        (.setProperty "top" (str (.-top rect) "px"))
        (.setProperty "left" (str (.-left rect) "px"))
        (.setProperty "height" "auto"))
      (set! (.-textContent mirror) (subs text 0 index))
      ;; zero-width space, so the marker has a position even at a line end
      (set! (.-textContent marker) "​")
      (.appendChild mirror marker)
      (js/document.body.appendChild mirror)
      (let [marker-rect (.getBoundingClientRect marker)
            position {:top (- (.-top marker-rect) (.-scrollTop textarea))
                      :left (.-left marker-rect)}]
        (.remove mirror)
        position))))
