(ns heraldicon.heraldry.render
  (:require
   [clojure.string :as str]
   [heraldicon.context :as c]
   [heraldicon.frontend.component.tree :as-alias tree]
   [heraldicon.frontend.library.arms.details :as-alias arms.details]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.line.fimbriation :as fimbriation]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.interface :as interface]
   [heraldicon.math.core :as math]
   [heraldicon.render.outline :as outline]
   [re-frame.core :as rf]))

(def ^:private overlap-width
  0.1)

(defn- edge-outline? [context]
  (or (interface/render-option :outline? context)
      (= (interface/render-option :mode context) :hatching)
      (interface/get-sanitized-data (c/++ context :outline?))
      (= (interface/get-sanitized-data (c/++ context :outline-mode)) :keep)))

(defn shape-path [shape]
  (let [shape-path (:shape shape)]
    (cond->> shape-path
      (vector? shape-path) (apply str))))

(defn shape-mask [context overlap?]
  (let [render-shape (interface/get-render-shape context)]
    [:path (cond-> {:d (shape-path render-shape)
                    :clip-rule "evenodd"
                    :fill-rule "evenodd"
                    :fill "#fff"}
             overlap? (assoc :stroke "#fff"
                             :stroke-width overlap-width))]))

(defn- render-edge [context {:keys [lines paths]}]
  (let [outline? (edge-outline? context)]
    (if paths
      (when outline?
        (into [:g (outline/style context)]
              (map (fn [edge-path]
                     [:path {:d edge-path}]))
              paths))
      [line/render lines outline? context])))

(defn ordinary-edges [context]
  (let [{:keys [edges]} (interface/get-render-shape context)]
    (into [:g]
          (map (fn [edge]
                 [render-edge context edge]))
          edges)))

(defn charge-edges [context]
  (when (edge-outline? context)
    (let [{:keys [shape]} (interface/get-render-shape context)]
      (into [:g (outline/style context)]
            (map (fn [edge-path]
                   [:path {:d edge-path}]))
            shape))))

(defn field-edges [context]
  (let [edges (interface/get-field-edges context)]
    (into [:g]
          (map (fn [edge]
                 [render-edge context edge]))
          edges)))

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
        fimbriation (if (and (-> render-shape :edges (get 0) :paths)
                             (-> fimbriation :mode (or :none) (= :none))
                             (-> line-fimbriation :mode (or :none) (not= :none)))
                      line-fimbriation
                      fimbriation)
        fimbriation-shape (or fimbriation-shape
                              [:path {:d (shape-path render-shape)
                                      :clip-rule "evenodd"
                                      :fill-rule "evenodd"}])
        outline? (edge-outline? context)]
    [:<>
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

(defn shape-highlight [context]
  (when (and (not (:svg-export? (c/render-hints context)))
             (or @(rf/subscribe [::tree/node-highlighted?
                                 ::arms.details/identifier
                                 (conj (:path context) :field)])
                 @(rf/subscribe [::tree/node-highlighted?
                                 ::arms.details/identifier
                                 (:path context)])))
    [:path.node-highlighted {:d (str/join " " (:shape (interface/get-render-shape context)))
                             :style {:stroke-width 1
                                     :stroke-linecap "round"
                                     :stroke-linejoin "round"
                                     :fill "none"
                                     :pointer-events "none"}}]))
