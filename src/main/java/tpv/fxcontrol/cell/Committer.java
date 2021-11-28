package tpv.fxcontrol.cell;

 interface Committer<T> {
     void commit(String s, T item);
     default String getText(T item){
         return item.toString();
     }
}
