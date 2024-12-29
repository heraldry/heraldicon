(ns heraldicon.frontend.element.charge-type-select
  (:require
   [clojure.string :as str]
   [heraldicon.context :as c]
   [heraldicon.frontend.component.tree :as tree]
   [heraldicon.frontend.context :as context]
   [heraldicon.frontend.debounce :as debounce]
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.element.submenu :as submenu]
   [heraldicon.frontend.filter :as filter]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.repository.charge-types :as repository.charge-types]
   [heraldicon.frontend.status :as status]
   [heraldicon.interface :as interface]
   [re-frame.core :as rf]
   [reagent.core :as r]))

(def charge-types-path
  (conj repository.charge-types/db-path-charge-types :data))

(def base-context
  (c/<< context/default :path charge-types-path))

(def search-db-path
  [::search])

(rf/reg-sub ::search-title
  :<- [:get search-db-path]

  (fn [search-string [_ title]]
    (let [title (filter/normalize-string-for-match (or title ""))
          words (filter/split-search-string (or search-string ""))]
      (if (empty? words)
        true
        (reduce (fn [_ word]
                  (when (filter/matches-word? title word)
                    (reduced true)))
                nil
                words)))))
(defn- search
  [title]
  @(rf/subscribe [::search-title title]))

(defn- force-open?
  []
  (-> @(rf/subscribe [:get search-db-path])
      count
      pos?))

(rf/reg-event-fx ::update-search-field
  (fn [_ [_ value]]
    {::debounce/dispatch [::debounce-update-search-field [:set search-db-path value] 250]}))

(defn- search-field
  []
  (let [tmp-value (r/atom @(rf/subscribe [:get search-db-path]))]
    (fn []
      [:input {:type "search"
               :placeholder "Search"
               :value @tmp-value
               :style {:width "20em"}
               :on-change #(do
                             (reset! tmp-value (-> % .-target .-value))
                             (rf/dispatch [::update-search-field @tmp-value]))}])))

(defn- search-bar
  []
  [:div {:style {:margin-bottom "10px"}}
   [search-field]])

(rf/reg-event-db ::set-charge-type
  (fn [db [_ context path]]
    (let [attribute-path (:path context)
          {:keys [name]} (get-in db path)]
      (assoc-in db attribute-path name))))

(rf/reg-sub ::top-level-charge-type-paths
  :<- [:get (conj charge-types-path :types)]

  (fn [charge-types _]
    (->> charge-types
         (map-indexed vector)
         (sort-by #(some-> % second :name str/lower-case))
         (map (fn [[idx _]]
                (conj charge-types-path :types idx)))
         (into []))))

(defn- get-charge-type-path
  [charge-types charge-type]
  (let [charge-type (str/lower-case charge-type)
        path-nodes (tree-seq
                    #(contains? (second %) :types)
                    (fn [[path node]]
                      (map-indexed
                       (fn [idx child]
                         [(conj path :types idx) child])
                       (:types node)))
                    [[] charge-types])
        found-path (->> path-nodes
                        (filter (fn [[_ node]]
                                  (and (map? node)
                                       (some-> node :name clojure.string/lower-case (= charge-type)))))
                        first
                        first)]
    (when found-path
      (into charge-types-path found-path))))

(rf/reg-event-db ::set-active-node
  (fn [db [_ charge-types context]]
    (let [charge-type (get-in db (:path context))]
      (tree/select-node db ::identifier (get-charge-type-path charge-types charge-type) true))))

(defn- charge-type-select
  [context]
  (let [{:keys [status error]} @(rf/subscribe [::repository.charge-types/data #(rf/dispatch [::set-active-node % context])])]
    (case status
      :loading [status/loading]
      :error [status/error-display error]
      [:div
       [search-bar]
       [tree/tree
        ::identifier
        @(rf/subscribe [::top-level-charge-type-paths])
        base-context
        :select-fn #(rf/dispatch [::set-charge-type context %])
        :search-fn search
        :force-open? (force-open?)]])))

(defmethod element/element :ui.element/charge-type-select [context]
  (when-let [option (interface/get-options context)]
    (let [current-value (interface/get-raw-data context)
          value (or current-value
                    :string.miscellaneous/none)
          {:ui/keys [label]} option]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu context :string.option/select-charge-type
         [:div
          [tr value]]
         {:style {:position "fixed"
                  :transform "none"}}
         [:div {:style {:width "25em"
                        :height "25em"
                        :overflow-y "auto"}}
          [charge-type-select context]]]]])))
