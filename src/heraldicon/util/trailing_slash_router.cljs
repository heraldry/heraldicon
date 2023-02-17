(ns heraldicon.util.trailing-slash-router
  (:require
   [clojure.string :as str]
   [reitit.core :as r]
   [reitit.frontend :as reif]))

(defn create [routes]
  (let [parent (reif/router routes)]
    ^{:type ::r/router}
    (reify r/Router
      (router-name [_]
        :trailing-slash-handler)
      (routes [_]
        (r/routes parent))
      (compiled-routes [_]
        (r/compiled-routes parent))
      (options [_]
        (r/options parent))
      (route-names [_]
        (r/route-names parent))
      (match-by-path [_ path]
        (or (r/match-by-path parent path)
            (if (str/ends-with? path "/")
              (r/match-by-path parent (subs path 0 (dec (count path))))
              (r/match-by-path parent (str path "/")))))
      (match-by-name [_ name]
        (r/match-by-name parent name))
      (match-by-name [_ name params]
        (r/match-by-name parent name params)))))

(defn fix-path-in-address-bar [path]
  (let [real-path (.. js/window -location -pathname)]
    (when (and path
               (not= path real-path))
      (some-> js/window.history (.replaceState nil nil path)))))
