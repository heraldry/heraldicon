(ns heraldicon.heraldry.ordinary.render
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.render :as render]
   [heraldicon.interface :as interface]
   [heraldicon.render.ir :as ir]
   [heraldicon.util.uid :as uid]))

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

(defmethod interface/render-ir :heraldry/cottise [context]
  ((get-method interface/render-ir :heraldry/ordinary) context))

(defmethod interface/render-component :heraldry/cottise [context]
  [interface/render-ir-root context])

(defn- cottising [context]
  (let [cottising-context (c/++ context :cottising)
        cottise-1? (interface/get-raw-data (c/++ (c/++ cottising-context :cottise-1) :type))
        cottise-2? (interface/get-raw-data (c/++ (c/++ cottising-context :cottise-2) :type))
        cottise-opposite-1? (interface/get-raw-data (c/++ (c/++ cottising-context :cottise-opposite-1) :type))
        cottise-opposite-2? (interface/get-raw-data (c/++ (c/++ cottising-context :cottise-opposite-2) :type))
        cottise-extra-1? (interface/get-raw-data (c/++ (c/++ cottising-context :cottise-extra-1) :type))
        cottise-extra-2? (interface/get-raw-data (c/++ (c/++ cottising-context :cottise-extra-2) :type))]
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

(defmethod interface/render-ir :heraldry/ordinary [context]
  (let [{:keys [svg-export?
                clip?]} (c/render-hints context)
        clip-path-id (uid/generate "clip")
        {:keys [transform]} (interface/get-properties context)
        mask? (and svg-export?
                   (not clip?))
        field-context (c/++ context :field)
        ;; a flat-coloured field's shape is painted directly
        flat-colour (render/direct-fill-colour field-context)]
    (-> ir/blank
        (cond->
          (not flat-colour) (-> (ir/add-def [(if mask? :mask :clipPath) {:id clip-path-id}
                                             [render/shape-mask context]])
                                (ir/add-child field-context)))
        (assoc :body [:<>
                      [render/shape-fimbriation context]
                      (if flat-colour
                        [render/shape-fill context field-context flat-colour false]
                        [:g {(if mask? :mask :clip-path) (str "url(#" clip-path-id ")")}
                         (let [wrapper (if transform
                                         [:g {:transform transform}]
                                         [:<>])]
                           (conj wrapper [interface/ir-node field-context]))])
                      [render/ordinary-edges context]
                      [cottising context]
                      [render/shape-highlight context]]))))

(defmethod interface/render-component :heraldry/ordinary [context]
  [interface/render-ir-root context])
