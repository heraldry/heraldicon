(ns heraldicon.frontend.charge-types
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.context :as c]
   [heraldicon.entity.user :as entity.user]
   [heraldicon.frontend.api :as api]
   [heraldicon.frontend.component.tree :as tree]
   [heraldicon.frontend.context :as context]
   [heraldicon.frontend.debounce :as debounce]
   [heraldicon.frontend.history.core :as history]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.message :as message]
   [heraldicon.frontend.repository.charge-types-for-editing :as repository.charge-types-for-editing]
   [heraldicon.frontend.search-string :as search-string]
   [heraldicon.frontend.status :as status]
   [heraldicon.frontend.title :as title]
   [heraldicon.frontend.user.form.core :as form]
   [heraldicon.frontend.user.session :as session]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [taoensso.timbre :as log]))

(def form-db-path
  (form/form-path ::form))

(def search-db-path
  (conj form-db-path ::search))

(def show-deleted-and-empty-db-path
  (conj form-db-path ::show-deleted-and-empty))

(def save-button-clicked-db-path
  (conj form-db-path ::save-button-clicked))

(history/register-undoable-path form-db-path)

(def base-context
  (-> context/default
      (c/<< :path form-db-path)
      (c/<< :ns ::ns)))

(rf/reg-event-fx ::save
  (fn [{:keys [db]} _]
    (let [session (session/data-from-db db)
          data (get-in db form-db-path)]
      (go
        (try
          (let [result (<? (api/call :save-charge-types data session))]
            (rf/dispatch [:set form-db-path result])
            (rf/dispatch-sync [::message/set-success ::id "Updated"])
            (js/setTimeout #(rf/dispatch [::message/clear ::id]) 3000))

          (catch :default error
            (rf/dispatch [::message/set-error ::id (:message (ex-data error))])
            (log/error error "save charge-types error")))

        (rf/dispatch [:set save-button-clicked-db-path false]))

      {})))

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
   [search-field]
   [:div {:style {:display "inline-block"
                  :margin-left "10px"}}
    [:input {:type "checkbox"
             :id "show-checkbox"
             :checked (or @(rf/subscribe [:get show-deleted-and-empty-db-path]) false)
             :on-change #(let [new-checked? (-> % .-target .-checked)]
                           (rf/dispatch [:set show-deleted-and-empty-db-path new-checked?]))}]
    [:label.for-checkbox {:for "show-checkbox"} "Show deleted and empty"]]])

(defn- force-open?
  []
  (-> @(rf/subscribe [:get search-db-path])
      count
      pos?))

(rf/reg-sub ::search-title
  :<- [:get search-db-path]

  (fn [search-string [_ title]]
    (let [title (search-string/normalize-string-for-match (or title ""))
          words (search-string/split (or search-string ""))]
      (if (empty? words)
        true
        (reduce (fn [_ word]
                  (when (search-string/matches-word? title word)
                    (reduced true)))
                nil
                words)))))

(defn- search
  [title]
  @(rf/subscribe [::search-title title]))

(rf/reg-sub ::filter-by-deleted-and-empty
  (fn [[_ path]]
    [(rf/subscribe [:get show-deleted-and-empty-db-path])
     (rf/subscribe [:get path])])

  (fn [[show-deleted-and-empty-flag {:keys [metadata charge_count]}] _]
    (or show-deleted-and-empty-flag
        (not (and (:deleted? metadata)
                  (zero? charge_count))))))

(defn- filter-by-deleted-and-empty
  [path]
  @(rf/subscribe [::filter-by-deleted-and-empty path]))

(defn- save-button
  []
  (let [disabled? @(rf/subscribe [:get save-button-clicked-db-path])]
    [:button.button.primary {:type "submit"
                             :on-click (fn [event]
                                         (.preventDefault event)
                                         (.stopPropagation event)
                                         (rf/dispatch-sync [:set save-button-clicked-db-path true])
                                         (rf/dispatch [::save]))
                             :disabled disabled?
                             :class (when disabled?
                                      "disabled")
                             :style {:float "right"
                                     :margin-bottom "10px"}}
     [tr :string.button/save]]))

(defn- charge-type-editor
  []
  (rf/dispatch [::title/set :string.menu/charge-types])
  [status/default
   (rf/subscribe [::repository.charge-types-for-editing/data #(rf/dispatch [:set form-db-path %])])
   (fn [_]
     [:div {:style {:display "grid"
                    :grid-gap "10px"
                    :grid-template-columns "[start] 40em [first] 40em [end]"
                    :grid-template-rows "[top] 100% [bottom]"
                    :grid-template-areas "'left right'"
                    :height "calc(100% - 20px)"
                    :padding "10px"}}
      [:div {:style {:grid-area "left"
                     :position "relative"}}
       [:div {:style {:height "calc(100% - 2.5em)"
                      :overflow "scroll"}}
        [search-bar]
        [history/buttons form-db-path]
        [tree/tree
         ::identifier
         [form-db-path] base-context
         :search-fn search
         :filter-fn filter-by-deleted-and-empty
         :force-open? (force-open?)]]
       [save-button]
       [:div {:style {:width "80%"
                      :float "left"}}
        [message/display ::id]]]

      [:div {:style {:grid-area "right"
                     :position "relative"}}
       [:div {:style {:height "calc(100%)"
                      :overflow "scroll"}}
        [tree/tree
         ::identifier
         [form-db-path] base-context
         :filter-fn filter-by-deleted-and-empty
         :extra :second]]]])])

(defn view []
  (if (entity.user/admin? @(rf/subscribe [::session/data]))
    [charge-type-editor]
    [:div]))

(rf/reg-sub ::name-map
  :<- [:get form-db-path]

  (fn [data _]
    (let [types (tree-seq
                 (fn [[data _path]]
                   (:type data))
                 (fn [[data path]]
                   (map-indexed (fn [index item]
                                  [item (conj path index)])
                                (:types data)))
                 [data form-db-path])
          name-map (-> (group-by (comp :name first) types)
                       (update-vals (fn [value]
                                      (mapv (fn [[_item path]]
                                              path)
                                            value))))]
      name-map)))

(rf/reg-sub ::undeleted-child?
  (fn [[_ {:keys [path]}]]
    (rf/subscribe [:get path]))

  (fn [data _]
    (some (comp not :deleted? :metadata)
          (tree-seq (every-pred map? :type) :types data))))
