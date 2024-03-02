(ns heraldicon.heraldry.ordinary.render
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.render :as render]
   [heraldicon.interface :as interface]
   [heraldicon.util.uid :as uid]
   [re-frame.core :as rf]))

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
                             :cottise-opposite-2
                             :cottise-extra-1
                             :cottise-extra-2])]
    (update context :cottise-parts merge cottise-parts)))

(defn- render-cottise [context]
  (let [{:keys [num-cottise-parts]
         :or {num-cottise-parts 1}} (interface/get-properties (c/-- context 2))]
    [:<>
     (into [:<>]
           (keep (fn [part]
                   (let [part-context (set-cottise-part context part)]
                     (when (interface/get-properties part-context)
                       [interface/render-component part-context]))))
           (range num-cottise-parts))
     [render/shape-highlight context]]))

(defmethod interface/render-component :heraldry/cottise [context]
  ((get-method interface/render-component :heraldry/ordinary) context))

(defn- cottising [context]
  (let [cottising-context (c/++ context :cottising)
        cottise-1? @(rf/subscribe [::cottise? (c/++ cottising-context :cottise-1)])
        cottise-2? @(rf/subscribe [::cottise? (c/++ cottising-context :cottise-2)])
        cottise-opposite-1? @(rf/subscribe [::cottise? (c/++ cottising-context :cottise-opposite-1)])
        cottise-opposite-2? @(rf/subscribe [::cottise? (c/++ cottising-context :cottise-opposite-2)])
        cottise-extra-1? @(rf/subscribe [::cottise? (c/++ cottising-context :cottise-extra-1)])
        cottise-extra-2? @(rf/subscribe [::cottise? (c/++ cottising-context :cottise-extra-2)])]
    [:<>
     (when cottise-1?
       [render-cottise (c/++ cottising-context :cottise-1)])
     (when cottise-opposite-1?
       [render-cottise (c/++ cottising-context :cottise-opposite-1)])
     (when cottise-extra-1?
       [render-cottise (c/++ cottising-context :cottise-extra-1)])

     (when (and cottise-1?
                cottise-2?)
       [render-cottise (c/++ cottising-context :cottise-2)])
     (when (and cottise-opposite-1?
                cottise-opposite-2?)
       [render-cottise (c/++ cottising-context :cottise-opposite-2)])
     (when (and cottise-extra-1?
                cottise-extra-2?)
       [render-cottise (c/++ cottising-context :cottise-extra-2)])]))

(defmethod interface/render-component :heraldry/ordinary [context]
  (let [{:keys [svg-export?]} (c/render-hints context)
        clip-path-id (uid/generate "clip")
        {:keys [transform]} (interface/get-properties context)]
    [:<>
     [:defs
      [(if svg-export?
         :mask
         :clipPath) {:id clip-path-id}
       [render/shape-mask context]]]
     [render/shape-fimbriation context]
     [:g {(if svg-export?
            :mask
            :clip-path) (str "url(#" clip-path-id ")")}
      (let [wrapper (if transform
                      [:g {:transform transform}]
                      [:<>])]
        (conj wrapper [interface/render-component (c/++ context :field)]))]
     [render/ordinary-edges context]
     [cottising context]
     [render/shape-highlight context]]))
