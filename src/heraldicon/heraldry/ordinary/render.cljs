(ns heraldicon.heraldry.ordinary.render
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.shared :as field.shared]
   [heraldicon.heraldry.line.fimbriation :as fimbriation]
   [heraldicon.heraldry.render :as render]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.interface :as interface]
   [heraldicon.render.outline :as outline]
   [heraldicon.util.uid :as uid]
   [re-frame.core :as rf]))

(defn- fimbriation [context]
  ;; TODO: this should move elsewhere and be merged with charge.other
  (let [render-shape (interface/get-render-shape context)
        fimbriation (interface/get-sanitized-data (c/++ context :fimbriation))
        line-fimbriation (interface/get-sanitized-data (c/++ context :line :fimbriation))
        fimbriation (if (and (-> render-shape :lines (get 0) :edge-paths)
                             (-> fimbriation :mode (or :none) (= :none))
                             (-> line-fimbriation :mode (or :none) (not= :none)))
                      line-fimbriation
                      fimbriation)
        fimbriation-shape [:path {:d (render/shape-path render-shape)
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

(rf/reg-sub ::cottise?
  (fn [[_ {:keys [path]}] _]
    (rf/subscribe [:get (conj path :type)]))

  (fn [cottise? _]
    cottise?))

(defn- set-cottise-part [context part]
  (let [cottising-path (-> context c/-- :path)
        cottise-parts (into {}
                            (map (fn [cottise]
                                   [(conj cottising-path cottise) part]))
                            [:cottise-1
                             :cottise-2
                             :cottise-opposite-1
                             :cottise-opposite-2])]
    (update context :cottise-parts merge cottise-parts)))

(defn- render-cottise [context]
  (let [{:keys [num-cottise-parts]
         :or {num-cottise-parts 1}} (interface/get-properties (c/-- context 2))]
    (into [:g]
          (keep (fn [part]
                  (let [part-context (set-cottise-part context part)]
                    (when (interface/get-properties part-context)
                      [interface/render-component part-context]))))
          (range num-cottise-parts))))

(defmethod interface/render-component :heraldry/cottise [context]
  ((get-method interface/render-component :heraldry/ordinary) context))

(defn- cottising [context]
  (let [cottising-context (c/++ context :cottising)
        cottise-1? @(rf/subscribe [::cottise? (c/++ cottising-context :cottise-1)])
        cottise-2? @(rf/subscribe [::cottise? (c/++ cottising-context :cottise-2)])
        cottise-opposite-1? @(rf/subscribe [::cottise? (c/++ cottising-context :cottise-opposite-1)])
        cottise-opposite-2? @(rf/subscribe [::cottise? (c/++ cottising-context :cottise-opposite-2)])]
    [:g
     (when cottise-1?
       [render-cottise (c/++ cottising-context :cottise-1)])
     (when cottise-opposite-1?
       [render-cottise (c/++ cottising-context :cottise-opposite-1)])

     (when (and cottise-1?
                cottise-2?)
       [render-cottise (c/++ cottising-context :cottise-2)])
     (when (and cottise-opposite-1?
                cottise-opposite-2?)
       [render-cottise (c/++ cottising-context :cottise-opposite-2)])]))

(defmethod interface/render-component :heraldry/ordinary [{:keys [svg-export?]
                                                           :as context}]
  (let [clip-path-id (uid/generate "clip")
        {:keys [transform]} (interface/get-properties context)]
    [:g
     [:defs
      [(if svg-export?
         :mask
         :clipPath) {:id clip-path-id}
       [render/shape-mask context]]]
     [fimbriation context]
     [:g {(if svg-export?
            :mask
            :clip-path) (str "url(#" clip-path-id ")")}
      [:g (when transform
            {:transform transform})
       [field.shared/render (c/++ context :field)]]]
     [render/ordinary-edges context]
     [cottising context]]))
