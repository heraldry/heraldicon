(ns heraldicon.heraldry.render
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.line.fimbriation :as fimbriation]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.interface :as interface]
   [heraldicon.math.core :as math]
   [heraldicon.render.outline :as outline]))

(defn- edge-outline? [context]
  (or (interface/render-option :outline? context)
      (= (interface/render-option :mode context) :hatching)
      (interface/get-sanitized-data (c/++ context :outline?))
      (= (interface/get-sanitized-data (c/++ context :outline-mode)) :keep)))

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

(defn- render-line [context {:keys [segments edge-paths]}]
  (let [outline? (edge-outline? context)]
    (if edge-paths
      (when outline?
        (into [:g (outline/style context)]
              (map (fn [edge-path]
                     [:path {:d edge-path}]))
              edge-paths))
      [line/render segments outline? context])))

(defn ordinary-edges [context]
  (let [{:keys [lines]} (interface/get-render-shape context)]
    (into [:g]
          (map (fn [line]
                 [render-line context line]))
          lines)))

(defn charge-edges [context]
  (when (edge-outline? context)
    (let [{:keys [shape]} (interface/get-render-shape context)]
      (into [:g (outline/style context)]
            (map (fn [edge-path]
                   [:path {:d edge-path}]))
            shape))))

(defn field-edges [context]
  (let [lines (interface/get-field-edges context)]
    (into [:g]
          (map (fn [line]
                 [render-line context line]))
          lines)))

(defn shape-fimbriation [context & {:keys [fimbriation-shape reverse-transform scale]
                                    :or {scale 1}}]
  (let [render-shape (when-not fimbriation-shape
                       (interface/get-render-shape context))
        {:keys [width height]} (interface/get-parent-environment context)
        fimbriation-percentage-base (min width height)
        fimbriation (some-> (interface/get-sanitized-data (c/++ context :fimbriation))
                            (update :thickness-1 (partial math/percent-of fimbriation-percentage-base))
                            (update :thickness-2 (partial math/percent-of fimbriation-percentage-base)))
        line-fimbriation (interface/get-sanitized-data (c/++ context :line :fimbriation))
        fimbriation (if (and (-> render-shape :lines (get 0) :edge-paths)
                             (-> fimbriation :mode (or :none) (= :none))
                             (-> line-fimbriation :mode (or :none) (not= :none)))
                      line-fimbriation
                      fimbriation)
        fimbriation-shape (or fimbriation-shape
                              [:path {:d (shape-path render-shape)
                                      :clip-rule "evenodd"
                                      :fill-rule "evenodd"}])
        outline? (edge-outline? context)]
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
             :scale scale
             :reverse-transform reverse-transform
             :corner (:corner fimbriation)])
          [fimbriation/dilate-and-fill
           fimbriation-shape
           (cond-> thickness
             outline? (- (/ outline/stroke-width 2)))
           (-> fimbriation
               :tincture-2
               (tincture/pick context)) context
           :scale scale
           :reverse-transform reverse-transform
           :corner (:corner fimbriation)]]))
     (when (-> fimbriation :mode #{:single :double})
       (let [thickness (:thickness-1 fimbriation)]
         [:<>
          (when outline?
            [fimbriation/dilate-and-fill
             fimbriation-shape
             (+ thickness (/ outline/stroke-width 2))
             (outline/color context) context
             :scale scale
             :reverse-transform reverse-transform
             :corner (:corner fimbriation)])
          [fimbriation/dilate-and-fill
           fimbriation-shape
           (cond-> thickness
             outline? (- (/ outline/stroke-width 2)))
           (-> fimbriation
               :tincture-1
               (tincture/pick context)) context
           :scale scale
           :reverse-transform reverse-transform
           :corner (:corner fimbriation)]]))]))
