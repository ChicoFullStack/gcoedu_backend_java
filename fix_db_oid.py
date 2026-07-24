import psycopg2

def fix_db():
    print("Conectando ao banco de dados para corrigir OID...")
    conn = psycopg2.connect("postgresql://postgres:postgres@179.197.232.2:5432/postgres")
    conn.autocommit = True
    cur = conn.cursor()
    
    cur.execute("SELECT table_schema FROM information_schema.tables WHERE table_name = 'answer_sheet_gabaritos'")
    schemas = [row[0] for row in cur.fetchall()]
    
    for schema in schemas:
        print(f"Atualizando schema: {schema}")
        try:
            # Drop as colunas bytea criadas incorretamente
            cur.execute(f"ALTER TABLE {schema}.answer_sheet_gabaritos DROP COLUMN IF EXISTS template_block_1;")
            cur.execute(f"ALTER TABLE {schema}.answer_sheet_gabaritos DROP COLUMN IF EXISTS template_block_2;")
            
            # Adiciona como OID (que o @Lob do Hibernate mapeia no PostgreSQL)
            cur.execute(f"ALTER TABLE {schema}.answer_sheet_gabaritos ADD COLUMN template_block_1 OID;")
            cur.execute(f"ALTER TABLE {schema}.answer_sheet_gabaritos ADD COLUMN template_block_2 OID;")
            print(f"Schema '{schema}' corrigido para OID com sucesso!")
        except Exception as e:
            print(f"Erro no schema '{schema}': {e}")
            
    cur.close()
    conn.close()
    print("Pronto!")

if __name__ == '__main__':
    fix_db()
