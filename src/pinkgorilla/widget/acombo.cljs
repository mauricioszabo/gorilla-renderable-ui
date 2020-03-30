(ns pinkgorilla.widget.acombo
  (:require
    ;[taoensso.timbre :refer-macros (info)]
   [pinkgorilla.ui.pinkie :refer [register-tag]]))

(defn info [str]
  (.log js/console str))

; COMBOBOX 2

(defn on-combo-changed- [value-atom _ action event]
  (let [;(.log js/console "selected index: " (.indexOf list value))
        value (.. event -target -value)
        ;; index (.. event -target -selectedIndex)
        ]
    (reset! value-atom value)
    (action value)))

(defn list-selector
  "combobox that is bound to an external atom.
      list is supplied"
  ([value-atom list action]
   (let [keys  {:on-change #(on-combo-changed- value-atom  list action %)} ;  #(reset! value-atom (.. % -target -value))
         keys  (if (nil? @value-atom) keys (assoc keys :value @value-atom))]
     [:select keys :value
      (when list (map-indexed (fn [idx item] [:option {:key idx :value item} item]) list))]))
  ([value-atom list]
   (list-selector value-atom list #(info (str "list selected: " %)))))

(defn assoc-selected [props current-val val]
  (if (= current-val val)
    (assoc props :selected true)
    props))

(defn go-next [value-atom list action]
  (let [new-index (inc (.indexOf list @value-atom))
        new-index (if (= new-index (count list)) 0 new-index)
        new-value (nth list new-index)]
    (reset! value-atom new-value)
    (action @value-atom)))

(defn go-prior [value-atom list action]
  (let [new-index (dec (.indexOf list @value-atom))
        new-index (if (< new-index 0) (- (count list) 1) new-index)
        new-value (nth list new-index)]
    (reset! value-atom new-value)
    (action @value-atom)))

(register-tag :p/acombo list-selector)
