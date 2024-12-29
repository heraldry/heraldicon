(ns heraldicon.frontend.component.ordinary
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.component.core :as component]
   [heraldicon.frontend.component.cottise :as cottise]
   [heraldicon.frontend.component.drag :as drag]
   [heraldicon.frontend.component.element :as component.element]
   [heraldicon.frontend.component.tree :as tree]
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.frontend.validation :as validation]
   [heraldicon.heraldry.default :as default]
   [heraldicon.heraldry.ordinary.core :as ordinary]
   [heraldicon.interface :as interface]
   [heraldicon.static :as static]
   [re-frame.core :as rf]))

(macros/reg-event-db ::remove-cottise
  (fn [db [_ {:keys [path]}]]
    (let [cottise (last path)
          path (-> path drop-last vec)]
      (-> db
          (cond->
            (= cottise :cottise-1) (->
                                     (update-in path dissoc :cottise-2)
                                     (assoc-in (conj path :cottise-1)
                                               (get-in db (conj path :cottise-2))))

            (= cottise :cottise-2) (update-in path dissoc :cottise-2)

            (= cottise :cottise-opposite-1) (->
                                              (update-in path dissoc :cottise-opposite-2)
                                              (assoc-in (conj path :cottise-opposite-1)
                                                        (get-in db (conj path :cottise-opposite-2))))

            (= cottise :cottise-opposite-2) (update-in path dissoc :cottise-opposite-2)

            (= cottise :cottise-extra-1) (->
                                           (update-in path dissoc :cottise-extra-2)
                                           (assoc-in (conj path :cottise-extra-1)
                                                     (get-in db (conj path :cottise-extra-2))))

            (= cottise :cottise-extra-2) (update-in path dissoc :cottise-extra-2))
          (tree/change-selected-component-if-removed (-> path drop-last vec))))))

(defn- form [context]
  (element/elements
   context
   [:adapt-to-ordinaries?
    :type
    :origin
    :anchor
    :orientation
    :variant
    :positioning-mode
    :line
    :opposite-line
    :extra-line
    :escutcheon
    :angle
    :geometry
    :distance
    :thickness
    :corner-radius
    :smoothing
    :num-points
    :humetty
    :voided
    :fimbriation
    :outline?
    :manual-blazon]))

(defmethod component/node :heraldry/ordinary [{::tree/keys [identifier]
                                               :as context}]
  (let [ordinary-type (interface/get-raw-data (c/++ context :type))
        cottising-options (interface/get-options (c/++ context :cottising))
        cottise-1 (interface/get-raw-data (c/++ context :cottising :cottise-1))
        cottise-2 (interface/get-raw-data (c/++ context :cottising :cottise-2))
        cottise-opposite-1 (interface/get-raw-data (c/++ context :cottising :cottise-opposite-1))
        cottise-opposite-2 (interface/get-raw-data (c/++ context :cottising :cottise-opposite-2))
        cottise-extra-1 (interface/get-raw-data (c/++ context :cottising :cottise-extra-1))
        cottise-extra-2 (interface/get-raw-data (c/++ context :cottising :cottise-extra-2))
        cottise-1? (and (:cottise-1 cottising-options)
                        cottise-1)
        cottise-2? (and (:cottise-2 cottising-options)
                        cottise-1
                        cottise-2)
        cottise-opposite-1? (and (:cottise-opposite-1 cottising-options)
                                 cottise-opposite-1)
        cottise-opposite-2? (and (:cottise-opposite-2 cottising-options)
                                 cottise-opposite-1
                                 cottise-opposite-2)
        cottise-extra-1? (and (:cottise-extra-1 cottising-options)
                              cottise-extra-1)
        cottise-extra-2? (and (:cottise-extra-2 cottising-options)
                              cottise-extra-1
                              cottise-extra-2)
        cottise-1-context (c/++ context :cottising :cottise-1)
        cottise-2-context (c/++ context :cottising :cottise-2)
        cottise-opposite-1-context (c/++ context :cottising :cottise-opposite-1)
        cottise-opposite-2-context (c/++ context :cottising :cottise-opposite-2)
        cottise-extra-1-context (c/++ context :cottising :cottise-extra-1)
        cottise-extra-2-context (c/++ context :cottising :cottise-extra-2)
        menu (cond-> []
               (and (:cottise-1 cottising-options)
                    (not cottise-1?))
               (conj {:title (cottise/cottise-name cottise-1-context)
                      :handler #(do
                                  (rf/dispatch-sync [:set cottise-1-context default/cottise])
                                  (rf/dispatch [::tree/select-node identifier (:path cottise-1-context) true]))})

               (and (:cottise-2 cottising-options)
                    cottise-1?
                    (not cottise-2?))
               (conj {:title (cottise/cottise-name cottise-2-context)
                      :handler #(do
                                  (rf/dispatch-sync [:set cottise-2-context default/cottise])
                                  (rf/dispatch [::tree/select-node identifier (:path cottise-2-context) true]))})

               (and (:cottise-opposite-1 cottising-options)
                    (not cottise-opposite-1?))
               (conj {:title (cottise/cottise-name cottise-opposite-1-context)
                      :handler #(do
                                  (rf/dispatch-sync [:set cottise-opposite-1-context default/cottise])
                                  (rf/dispatch [::tree/select-node identifier (:path cottise-opposite-1-context) true]))})

               (and (:cottise-opposite-2 cottising-options)
                    cottise-opposite-1?
                    (not cottise-opposite-2?))
               (conj {:title (cottise/cottise-name cottise-opposite-2-context)
                      :handler #(do
                                  (rf/dispatch-sync [:set cottise-opposite-2-context default/cottise])
                                  (rf/dispatch [::tree/select-node identifier (:path cottise-opposite-2-context) true]))})

               (and (:cottise-extra-1 cottising-options)
                    (not cottise-extra-1?))
               (conj {:title (cottise/cottise-name cottise-extra-1-context)
                      :handler #(do
                                  (rf/dispatch-sync [:set cottise-extra-1-context default/cottise])
                                  (rf/dispatch [::tree/select-node identifier (:path cottise-extra-1-context) true]))})

               (and (:cottise-extra-2 cottising-options)
                    cottise-extra-1?
                    (not cottise-extra-2?))
               (conj {:title (cottise/cottise-name cottise-extra-2-context)
                      :handler #(do
                                  (rf/dispatch-sync [:set cottise-extra-2-context default/cottise])
                                  (rf/dispatch [::tree/select-node identifier (:path cottise-extra-2-context) true]))}))]
    {:title (ordinary/title context)
     :icon {:default (static/static-url
                      (str "/svg/ordinary-type-" (name ordinary-type) "-unselected.svg"))
            :selected (static/static-url
                       (str "/svg/ordinary-type-" (name ordinary-type) "-selected.svg"))}
     :validation (validation/validate-ordinary context)
     :buttons [{:icon "fas fa-plus"
                :title :string.button/add
                :disabled? (empty? menu)
                :menu menu}
               {:icon "far fa-clone"
                :title :string.button/duplicate
                :handler #(rf/dispatch [::component.element/duplicate context])}]
     :draggable? true
     :drop-options-fn drag/drop-options
     :drop-fn drag/drop-fn
     :nodes (cond-> [{:context (c/++ context :field)}]
              cottise-2? (conj {:context cottise-2-context
                                :buttons [{:icon "far fa-trash-alt"
                                           :remove? true
                                           :title :string.tooltip/remove
                                           :handler #(rf/dispatch [::remove-cottise cottise-2-context])}]})
              cottise-1? (conj {:context cottise-1-context
                                :buttons [{:icon "far fa-trash-alt"
                                           :remove? true
                                           :title :string.tooltip/remove
                                           :handler #(rf/dispatch [::remove-cottise cottise-1-context])}]})
              cottise-opposite-1? (conj {:context cottise-opposite-1-context
                                         :buttons [{:icon "far fa-trash-alt"
                                                    :remove? true
                                                    :title :string.tooltip/remove
                                                    :handler #(rf/dispatch [::remove-cottise cottise-opposite-1-context])}]})

              cottise-opposite-2? (conj {:context cottise-opposite-2-context
                                         :buttons [{:icon "far fa-trash-alt"
                                                    :remove? true
                                                    :title :string.tooltip/remove
                                                    :handler #(rf/dispatch [::remove-cottise cottise-opposite-2-context])}]})
              cottise-extra-1? (conj {:context cottise-extra-1-context
                                      :buttons [{:icon "far fa-trash-alt"
                                                 :remove? true
                                                 :title :string.tooltip/remove
                                                 :handler #(rf/dispatch [::remove-cottise cottise-extra-1-context])}]})

              cottise-extra-2? (conj {:context cottise-extra-2-context
                                      :buttons [{:icon "far fa-trash-alt"
                                                 :remove? true
                                                 :title :string.tooltip/remove
                                                 :handler #(rf/dispatch [::remove-cottise cottise-extra-2-context])}]}))}))

(defmethod component/form :heraldry/ordinary [_context]
  form)
