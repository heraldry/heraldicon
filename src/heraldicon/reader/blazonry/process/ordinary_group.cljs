(ns heraldicon.reader.blazonry.process.ordinary-group
  (:require
   [clojure.walk :as walk]))

(defn- replace-adjusted-components [hdn indexed-components]
  (update
   hdn
   :components
   (fn [components]
     (vec
      (loop [components components
             [[index component] & rest] indexed-components]
        (if index
          (recur
           (assoc components index component)
           rest)
          components))))))

(defn- arrange-ordinaries-in-one-dimension [hdn indexed-components & {:keys [offset-keyword
                                                                             spacing
                                                                             default-size]}]
  (let [num-elements (count indexed-components)
        size-without-spacing (->> indexed-components
                                  (map second)
                                  (map (fn [component]
                                         (-> component
                                             :geometry
                                             :size
                                             (or default-size))))
                                  (reduce +))
        total-size-with-margin (-> num-elements
                                   inc
                                   (* spacing)
                                   (+ size-without-spacing))
        stretch-factor (min 1
                            (/ 100 total-size-with-margin))
        total-size (-> num-elements
                       dec
                       (* spacing)
                       (+ size-without-spacing)
                       (* stretch-factor))
        adjusted-indexed-components (map-indexed (fn [index [parent-index component]]
                                                   (let [size (-> component :geometry :size (or default-size))
                                                         size-so-far (->> indexed-components
                                                                          (take index)
                                                                          (map second)
                                                                          (map (fn [component]
                                                                                 (-> component
                                                                                     :geometry
                                                                                     :size
                                                                                     (or default-size))))
                                                                          (reduce +))
                                                         offset (-> size-so-far
                                                                    (+ (* index spacing))
                                                                    (+ (/ size 2))
                                                                    (* stretch-factor)
                                                                    (- (/ total-size 2)))]
                                                     [parent-index
                                                      (-> component
                                                          (assoc-in [:geometry :size] (* size stretch-factor))
                                                          (assoc-in [:anchor offset-keyword] offset))]))
                                                 indexed-components)]
    (replace-adjusted-components hdn adjusted-indexed-components)))

(defn- arrange-orles [hdn indexed-components]
  (let [indexed-components indexed-components
        default-size 3
        spacing 2
        initial-spacing 3
        adjusted-indexed-components (map-indexed (fn [real-index [parent-index component]]
                                                   (let [index (min real-index 5)
                                                         size (-> component :geometry :size (or default-size))
                                                         size-so-far (->> indexed-components
                                                                          (take index)
                                                                          (map second)
                                                                          (map (fn [component]
                                                                                 (-> component
                                                                                     :thickness
                                                                                     (or default-size))))
                                                                          (reduce +))
                                                         offset (+ size-so-far
                                                                   initial-spacing
                                                                   (* index spacing))]
                                                     [parent-index
                                                      (assoc component
                                                             :thickness (if (-> component
                                                                                :line
                                                                                (or :straight)
                                                                                (not= :straight))
                                                                          (/ size 2)
                                                                          size)
                                                             :distance offset)]))
                                                 indexed-components)]
    (replace-adjusted-components hdn adjusted-indexed-components)))

(defn- arrange-ordinaries [hdn indexed-components]
  (let [ordinary-type (-> indexed-components
                          first
                          second
                          :type
                          name
                          keyword)]
    (case ordinary-type
      :pale (arrange-ordinaries-in-one-dimension hdn indexed-components
                                                 :offset-keyword :offset-x
                                                 :spacing 10
                                                 :default-size 20)
      :pile (arrange-ordinaries-in-one-dimension
             hdn
             (map (fn [[index component]]
                    [index (-> component
                               (assoc-in [:orientation :point] :angle)
                               (assoc-in [:orientation :angle] 0))])
                  indexed-components)
             :offset-keyword :offset-x
             :spacing 0
             :default-size 33.333333)
      :fess (arrange-ordinaries-in-one-dimension hdn indexed-components
                                                 :offset-keyword :offset-y
                                                 :spacing 5
                                                 :default-size 15)
      :bend (arrange-ordinaries-in-one-dimension
             hdn
             (map (fn [[index component]]
                    [index (-> component
                               (assoc-in [:orientation :point] :angle)
                               (assoc-in [:orientation :angle] 45))])
                  indexed-components)

             :offset-keyword :offset-y
             :spacing 15
             :default-size 15)
      :bend-sinister (arrange-ordinaries-in-one-dimension
                      hdn
                      (map (fn [[index component]]
                             [index (-> component
                                        (assoc-in [:orientation :point] :angle)
                                        (assoc-in [:orientation :angle] 45))])
                           indexed-components)

                      :offset-keyword :offset-y
                      :spacing 15
                      :default-size 15)
      :chevron (arrange-ordinaries-in-one-dimension
                hdn
                (map (fn [[index component]]
                       [index (-> component
                                  (assoc-in [:orientation :point] :angle)
                                  (assoc-in [:orientation :angle] 45)
                                  (assoc-in [:origin :point] :angle)
                                  (assoc-in [:origin :angle] 0))])
                     indexed-components)
                :offset-keyword :offset-y
                :spacing 15
                :default-size 15)
      :orle (arrange-orles hdn indexed-components)
      hdn)))

(defn- add-ordinary-defaults [hdn]
  (if (and (map? hdn)
           (some-> hdn :type namespace (= "heraldry.field.type")))
    (let [components-by-type (->> hdn
                                  :components
                                  (map-indexed vector)
                                  (group-by (comp :type second)))]
      (doall
       (loop [hdn hdn
              [[component-type indexed-components] & rest] components-by-type]
         (if component-type
           (recur (if (and (-> indexed-components count (> 1))
                           (-> component-type namespace (= "heraldry.ordinary.type")))
                    (arrange-ordinaries hdn indexed-components)
                    hdn)
                  rest)
           hdn))))
    hdn))

(defn process [hdn]
  (walk/postwalk add-ordinary-defaults hdn))
