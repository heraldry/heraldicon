(ns heraldicon.heraldry.render
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.interface :as interface]
   [heraldicon.render.outline :as outline]))

(defn shape-path [shape]
  (let [shape-path (:shape shape)]
    (cond->> shape-path
      (vector? shape-path) (apply str))))

(defn shape-mask [context]
  (let [render-shape (interface/get-render-shape context)]
    [:path {:d (shape-path render-shape)
            :clip-rule "evenodd"
            :fill-rule "evenodd"
            :fill "#fff"}]))

(defn- render-line [context {:keys [line line-data line-from edge-paths]}]
  (let [outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))]
    (if edge-paths
      (when outline?
        (into [:g (outline/style context)]
              (map (fn [edge-path]
                     [:path {:d edge-path}]))
              edge-paths))
      [line/render line line-data line-from outline? context])))

(defn ordinary-edges [context]
  (let [{:keys [lines]} (interface/get-render-shape context)]
    (into [:g]
          (map (fn [line]
                 [render-line context line]))
          lines)))

(defn charge-edges [context]
  (let [outline-mode (if (or (interface/render-option :outline? context)
                             (= (interface/render-option :mode context)
                                :hatching)) :keep
                         (interface/get-sanitized-data (c/++ context :outline-mode)))
        outline? (= outline-mode :keep)]
    (when outline?
      (let [{:keys [shape]} (interface/get-render-shape context)]
        (into [:g (outline/style context)]
              (map (fn [edge-path]
                     [:path {:d edge-path}]))
              shape)))))

(defn field-edges [context]
  (let [lines (interface/get-field-edges context)]
    (into [:g]
          (map (fn [line]
                 [render-line context line]))
          lines)))
