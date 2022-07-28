(ns heraldicon.heraldry.ordinary.render
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.shared :as field.shared]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.interface :as interface]
   [heraldicon.util.uid :as uid]))

(defn- shape-path [shape]
  (:shape shape))

(defn- shape-mask [context]
  (let [render-shape (interface/get-render-shape context)]
    [:path {:d (shape-path render-shape)
            :clip-rule "evenodd"
            :fill-rule "evenodd"
            :fill "#fff"}]))

(defn- render-line [context {:keys [line line-data line-start]}]
  (let [outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))]
    [line/render line line-data line-start outline? context]))

(defn- edges [context]
  (let [{:keys [lines]} (interface/get-render-shape context)]
    (into [:g]
          (map (fn [line]
                 [render-line context line]))
          lines)))

(defn render [{:keys [svg-export?]
               :as context}]
  (let [clip-path-id (uid/generate "clip")]
    [:g
     [:defs
      [(if svg-export?
         :mask
         :clipPath) {:id clip-path-id}
       [shape-mask context]]]
     [:g {(if svg-export?
            :mask
            :clip-path) (str "url(#" clip-path-id ")")}
      [field.shared/render (c/++ context :field)]]
     [edges context]]))
