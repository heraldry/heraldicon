(ns heraldicon.heraldry.render
  (:require
   [clojure.string :as str]
   [heraldicon.context :as c]
   [heraldicon.frontend.component.tree :as-alias tree]
   [heraldicon.frontend.js-event :as js-event]
   [heraldicon.frontend.library.arms.details :as-alias arms.details]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.line.fimbriation :as fimbriation]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.interface :as interface]
   [heraldicon.math.core :as math]
   [heraldicon.render.outline :as outline]
   [heraldicon.util.cache :as cache]
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

(defn- paint-directly?
  "In the editor a field highlighted by hovering it, or the component it
  belongs to, in the preview is masked, so it can show the highlight."
  [field-context]
  (or (:svg-export? (c/render-hints field-context))
      (not @(rf/subscribe [::tree/node-highlighted?
                           ::arms.details/identifier
                           (:path field-context)]))))

(defn direct-fill-colour
  "The colour to paint a field with directly, or nil if it needs the masked
  rendering, see tincture/flat-colour and paint-directly?."
  [field-context]
  (when (paint-directly? field-context)
    (tincture/flat-colour field-context)))

;; patterns are defined in user space units, so a pattern that fills a field's
;; shape directly carries the transforms the masked rendering applies to the
;; tinctured rect; ids count in the cache store, like charge defs
(def ^:private transformed-pattern
  (cache/memoize
   ::transformed-pattern
   (fn transformed-pattern [pattern-id pattern-transform]
     (let [id (str pattern-id "-"
                   (::pattern-count (swap! cache/store update ::pattern-count (fnil inc 0))))
           href (str "#" pattern-id)]
       [:pattern {:id id
                  :href href
                  :xlinkHref href
                  :patternTransform pattern-transform}]))))

(defn direct-fill
  "The fill to paint a field's shape with directly, a colour or a pattern url,
  and the pattern def it needs, if any, or nil if the field needs the masked
  rendering, see tincture/plain-fill and paint-directly?. transform is the
  transform the masked rendering applies to the field."
  [field-context transform]
  (when (paint-directly? field-context)
    (when-let [fill (tincture/plain-fill field-context)]
      (let [pattern-id (second (re-matches #"url\(#(.+)\)" fill))
            pattern-transform (str transform (tincture/pattern-transform field-context))]
        (if (and pattern-id
                 (not (str/blank? pattern-transform)))
          (let [pattern (transformed-pattern pattern-id pattern-transform)]
            {:fill (str "url(#" (-> pattern second :id) ")")
             :def pattern})
          {:fill fill})))))

(defn shape-fill
  "The shape of a field filled directly with a colour or pattern, instead of a
  mask or clip path around a tinctured rect, see direct-fill. With overlap? it's
  widened like the mask of an overlapping subfield. In the editor it reacts to
  the pointer like the field's rect did."
  [context field-context fill overlap?]
  (let [{:keys [svg-export?
                charge-preview?
                select-component-fn
                select-field-fn
                enter-component-fn
                leave-component-fn]} (c/render-hints field-context)
        render-shape (interface/get-render-shape context)]
    [:path (cond-> {:d (shape-path render-shape)
                    :fill-rule "evenodd"
                    :fill fill}
             overlap? (assoc :stroke fill
                             :stroke-width overlap-width)
             (not svg-export?) (assoc :on-click (when select-component-fn
                                                  (js-event/handled #(select-component-fn field-context)))
                                      :on-double-click (when select-field-fn
                                                         (js-event/handled #(select-field-fn field-context)))
                                      :on-mouse-enter (when enter-component-fn
                                                        (js-event/handled #(enter-component-fn field-context)))
                                      :on-mouse-leave (when leave-component-fn
                                                        (js-event/handled #(leave-component-fn field-context)))
                                      :style (when-not charge-preview?
                                               {:cursor "pointer"})))]))

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
