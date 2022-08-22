(ns heraldicon.heraldry.render
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.line.fimbriation :as fimbriation]
   [heraldicon.heraldry.tincture :as tincture]
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

(defn shape-fimbriation [context]
  (let [render-shape (interface/get-render-shape context)
        fimbriation (interface/get-sanitized-data (c/++ context :fimbriation))
        line-fimbriation (interface/get-sanitized-data (c/++ context :line :fimbriation))
        fimbriation (if (and (-> render-shape :lines (get 0) :edge-paths)
                             (-> fimbriation :mode (or :none) (= :none))
                             (-> line-fimbriation :mode (or :none) (not= :none)))
                      line-fimbriation
                      fimbriation)
        fimbriation-shape [:path {:d (shape-path render-shape)
                                  :clip-rule "evenodd"
                                  :fill-rule "evenodd"}]
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))]
    [:g
     (when (-> fimbriation :mode #{:double})
       (let [thickness (+ (:thickness-1 fimbriation)
                          (:thickness-2 fimbriation))]
         [:<>
          (when outline?
            [fimbriation/dilate-and-fill
             fimbriation-shape
             (+ thickness (/ outline/stroke-width 2))
             (outline/color context) context
             :corner (:corner fimbriation)])
          [fimbriation/dilate-and-fill
           fimbriation-shape
           (cond-> thickness
             outline? (- (/ outline/stroke-width 2)))
           (-> fimbriation
               :tincture-2
               (tincture/pick context)) context
           :corner (:corner fimbriation)]]))
     (when (-> fimbriation :mode #{:single :double})
       (let [thickness (:thickness-1 fimbriation)]
         [:<>
          (when outline?
            [fimbriation/dilate-and-fill
             fimbriation-shape
             (+ thickness (/ outline/stroke-width 2))
             (outline/color context) context
             :corner (:corner fimbriation)])
          [fimbriation/dilate-and-fill
           fimbriation-shape
           (cond-> thickness
             outline? (- (/ outline/stroke-width 2)))
           (-> fimbriation
               :tincture-1
               (tincture/pick context)) context
           :corner (:corner fimbriation)]]))]))
