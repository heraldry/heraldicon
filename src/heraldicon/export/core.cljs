(ns heraldicon.export.core
  "Renders exports outside the reactive app, in the backend and in the browser
  alike, so both produce the same markup."
  (:require
   ["paper" :as paper]
   [clojure.string :as str]
   [heraldicon.config :as config]
   [heraldicon.font :as font]
   [heraldicon.interface :as interface]
   [heraldicon.svg.markup :as markup]
   [heraldicon.util.cache :as cache]
   [heraldicon.util.uid :as uid]
   [re-frame.db]
   [re-frame.subs]
   [reagent.core :as r]
   [reagent.ratom]))

(defn activate-scope [scope]
  (when scope
    ^js (.activate scope)))

(defn clean-up-render-state
  "Disposes the reactions and clears the caches of the currently bound render
  state."
  []
  ;; in the browser a flush would re-render the mounted app against the bound
  ;; render state
  (when config/is_backend
    (r/flush))
  (doseq [reaction (vals @re-frame.subs/query->reaction)]
    (reagent.ratom/dispose! reaction))
  (re-frame.subs/clear-subscription-cache!))

(defn with-render-state
  "Calls f with fresh render state bound: app-db, subscriptions, caches and
  the id counter. Cleans the state up afterwards and returns f's result.

  f must be synchronous and return a finished result, such as a rendered
  string. A binding doesn't survive a park in a go block: other requests run
  in between and rebind the same vars, so do all async work before calling
  this.

  The paper scope is activated here and removed by its owner. Without one, the
  render gets its own: otherwise it would draw into whichever scope another
  request activated last, whose project may already be removed."
  [scope f]
  (let [own-scope (when-not scope
                    (doto (paper/PaperScope.)
                      (js-invoke "setup" (paper/Size. 500 500))))]
    (binding [re-frame.db/app-db (reagent.ratom/atom {})
              re-frame.subs/query->reaction (atom {})
              uid/current-id (atom 0)
              interface/*static-cache* (atom {})
              cache/store (atom {})]
      (try
        (activate-scope (or scope own-scope))
        (f)
        (finally
          (clean-up-render-state)
          (when own-scope
            (.remove (.-project own-scope))))))))

(defn set-data!
  "Stores data at path in the bound app-db."
  [path data]
  (swap! re-frame.db/app-db assoc-in path data))

(defn unique-path
  "A path in the render state's app-db for an entity, distinct for different
  data."
  [entity-data]
  [:export (keyword (str (hash entity-data)))])

(defn font-data-uri [font base64-data]
  (let [data-type (-> (font/path-map font)
                      str/lower-case
                      (str/split #"[.]")
                      last
                      {"ttf" "font/truetype"
                       "otf" "font/opentype"
                       "woff" "font/woff"})]
    (str "data:" data-type ";charset=utf-8;base64," base64-data)))

(defn- font->css [font-data-uris font]
  (when-let [font-name (font/font-map font)]
    (str "@font-face {\n"
         "  font-family: \"" font-name "\";\n"
         "  font-weight: 400;\n"
         "  src: url(\"" (font-data-uris font) "\");\n"
         "}\n")))

(defn embed-fonts
  "A style element with the fonts as data URIs; font-data-uris maps a font to
  its data URI."
  [font-data-uris fonts]
  (into [:style]
        (map (partial font->css font-data-uris))
        fonts))

(defn svg-markup
  "SVG exports are written by our own markup writer: they're rendered in server
  mode, where their components are plain functions."
  [hiccup]
  (str "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
       (markup/render hiccup)))
