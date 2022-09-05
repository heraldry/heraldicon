(ns heraldicon.heraldry.field.render
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.counterchange :as counterchange]
   [heraldicon.heraldry.render :as render]
   [heraldicon.heraldry.subfield :as subfield]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.interface :as interface]))

(defn- render-subfields [context {:keys [num-subfields transform overlap?-fn]
                                  :or {overlap?-fn even?}}]
  ;; TODO: overlap should move into subfield/render
  (into [:g]
        (map (fn [idx]
               ^{:key idx}
               [subfield/render (c/++ context :fields idx) transform (overlap?-fn idx)]))
        (sort-by overlap?-fn > (range num-subfields))))

(defn- field-path-allowed? [{:keys [path]
                             :as context}]
  (let [component-path (->> path
                            reverse
                            (drop-while (comp not int?)))
        index (first component-path)
        components-path (->> component-path
                             (drop 1)
                             reverse
                             vec)
        counterchanged-paths (c/counterchanged-paths context)
        length (count components-path)]
    (loop [[counterchanged-path & rest] counterchanged-paths]
      (if (nil? counterchanged-path)
        true
        (let [counterchanged-path (vec counterchanged-path)
              start (when (-> counterchanged-path count (>= length))
                      (subvec counterchanged-path 0 length))]
          (if (and (= start components-path)
                   (>= index (get counterchanged-path length)))
            false
            (recur rest)))))))

(defn- render-components [context]
  (into [:<>]
        (for [idx (range (interface/get-list-size (c/++ context :components)))
              :while (field-path-allowed? (c/++ context :components idx))]
          ^{:key idx}
          [interface/render-component (c/++ context :components idx)])))

(defn- render-counterchanged-field [{:keys [path]
                                     :as context} _properties]
  (when-let [parent-field-context (interface/get-counterchange-parent (interface/parent context))]
    (let [counterchange-tinctures (counterchange/tinctures parent-field-context)
          counterchanged-context (-> parent-field-context
                                     (c/add-counterchanged-path path)
                                     (c/add-counterchanged-tinctures counterchange-tinctures))]
      [interface/render-component counterchanged-context])))

(defn- render-plain-field [context _properties]
  [tincture/tinctured-field context])

(defmethod interface/render-component :heraldry/field [context]
  (let [{:keys [render-fn]
         field-type :type
         :as properties} (interface/get-properties context)
        render-fn (case field-type
                    :heraldry.field.type/plain render-plain-field
                    :heraldry.field.type/counterchanged render-counterchanged-field
                    (or render-fn
                        render-subfields))]
    [:<>
     [render-fn context properties]
     [render/field-edges context]
     [render-components context]]))
